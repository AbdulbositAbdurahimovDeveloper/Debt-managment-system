package uz.qarzdorlar_ai.service.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.exception.BadRequestException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.model.*;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionItemCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionUpdateDTO;
import uz.qarzdorlar_ai.repository.ExchangeRateRepository;
import uz.qarzdorlar_ai.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction calculation metodlar uchun service
 * Har bir transaction type uchun hisob-kitob logikasi
 */
@Service
@RequiredArgsConstructor
public class TransactionCalculationService {

    private final TransactionHelperService transactionHelperService;
    private final ProductRepository productRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final uz.qarzdorlar_ai.repository.ClientRepository clientRepository;

    /**
     * Transaction ni hisoblab beradi (create uchun)
     */
    public void calculateTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        switch (transaction.getType()) {
            case SALE -> calculateSaleTransaction(dto, transaction, client, currency);
            case PAYMENT -> calculatePaymentTransaction(dto, transaction, client, currency);
            case RETURN -> calculateReturnTransaction(dto, transaction, client, currency);
            case RETURN_PAYMENT -> calculateReturnPaymentTransaction(dto, transaction, client, currency);
            case PURCHASE -> calculatePurchaseTransaction(dto, transaction, client, currency);
            case TRANSFER -> calculateTransferTransaction(dto, transaction, client, currency);
            default -> throw new BadRequestException("Unsupported transaction type: " + transaction.getType());
        }
    }

    /**
     * Transaction ni qayta hisoblab beradi (update uchun)
     */
    public void recalculateTransaction(Transaction transaction, TransactionUpdateDTO dto) {
        Client client = transaction.getClient();
        Currency currency = transaction.getCurrency();

        // Exchange ratelarni olish (yangi yoki eski)
        BigDecimal exchangeRate = dto.getExchangeRate() != null 
                ? dto.getExchangeRate() 
                : transaction.getExchangeRate();
        
        BigDecimal clientExchangeRate = dto.getClientExchangeRate() != null 
                ? dto.getClientExchangeRate() 
                : transaction.getClientExchangeRate();

        // Validation - exchange ratelarni tekshirish
        transactionHelperService.validateExchangeRate(exchangeRate, currency);
        transactionHelperService.validateExchangeRate(clientExchangeRate, client.getBalanceCurrency());

        // Transaction type ga qarab qayta hisoblash
        switch (transaction.getType()) {
            case SALE -> recalculateSaleTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case PAYMENT -> recalculatePaymentTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case RETURN -> recalculateReturnTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case RETURN_PAYMENT -> recalculateReturnPaymentTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case PURCHASE -> recalculatePurchaseTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case TRANSFER -> recalculateTransferTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
        }
    }

    // ==================== CREATE METHODS ====================

    /**
     * SALE transaction type uchun logika
     * Mijozdan tovar sotiladi, mijozning qarzi ko'payadi (currentBalance kamayadi)
     */
    private void calculateSaleTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Items are required for SALE transaction");
        }

        BigDecimal exchangeRate = transactionHelperService.getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = transactionHelperService.getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            BigDecimal basePriceInTransactionCurrency = product.getPriceUsd().multiply(exchangeRate);
            BigDecimal unitPriceInTransactionCurrency = item.getUnitPrice() != null 
                    ? item.getUnitPrice() 
                    : basePriceInTransactionCurrency;
            
            // Custom price audit
            if (item.getUnitPrice() != null) {
                BigDecimal priceDifference = unitPriceInTransactionCurrency.subtract(basePriceInTransactionCurrency);
                BigDecimal priceDifferencePercent = basePriceInTransactionCurrency.compareTo(BigDecimal.ZERO) > 0
                        ? priceDifference.divide(basePriceInTransactionCurrency, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                
                if (priceDifferencePercent.abs().compareTo(BigDecimal.valueOf(5)) > 0) {
                    String auditNote = String.format(" [AUDIT: Custom price used. Base: %s, Actual: %s, Difference: %.2f%%]",
                            basePriceInTransactionCurrency, unitPriceInTransactionCurrency, priceDifferencePercent);
                    String currentDescription = transaction.getDescription() != null ? transaction.getDescription() : "";
                    transaction.setDescription(currentDescription + auditNote);
                }
            }

            BigDecimal itemUsdAmount;
            if ("USD".equals(currency.getCode())) {
                itemUsdAmount = unitPriceInTransactionCurrency.multiply(BigDecimal.valueOf(quantity));
            } else {
                itemUsdAmount = unitPriceInTransactionCurrency
                        .divide(exchangeRate, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(quantity));
            }

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);
            transactionItem.setUnitPrice(itemUsdAmount.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP));
            transactionItem.setTotalPrice(itemUsdAmount);

            transactionItems.add(transactionItem);
            totalUsdAmount = totalUsdAmount.add(itemUsdAmount);
        }

        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        // MUHIM: SALE holatida fee qarzga QO'SHILADI (qarz ko'payadi), ayirilmaydi
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.add(feeBalanceAmount));
        
        transaction.setItems(transactionItems);
    }

    /**
     * PAYMENT transaction type uchun logika
     */
    private void calculatePaymentTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        if (dto.getOriginalAmount() == null || dto.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for PAYMENT transaction");
        }

        BigDecimal exchangeRate = transactionHelperService.getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = transactionHelperService.getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());
        BigDecimal originalAmount = dto.getOriginalAmount();

        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    /**
     * RETURN transaction type uchun logika
     */
    private void calculateReturnTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Items are required for RETURN transaction");
        }

        BigDecimal exchangeRate = transactionHelperService.getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = transactionHelperService.getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            BigDecimal basePriceInTransactionCurrency = product.getPriceUsd().multiply(exchangeRate);
            BigDecimal unitPriceInTransactionCurrency = item.getUnitPrice() != null 
                    ? item.getUnitPrice() 
                    : basePriceInTransactionCurrency;
            
            if (item.getUnitPrice() != null) {
                BigDecimal priceDifference = unitPriceInTransactionCurrency.subtract(basePriceInTransactionCurrency);
                BigDecimal priceDifferencePercent = basePriceInTransactionCurrency.compareTo(BigDecimal.ZERO) > 0
                        ? priceDifference.divide(basePriceInTransactionCurrency, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                
                if (priceDifferencePercent.abs().compareTo(BigDecimal.valueOf(5)) > 0) {
                    String auditNote = String.format(" [AUDIT: Custom price used. Base: %s, Actual: %s, Difference: %.2f%%]",
                            basePriceInTransactionCurrency, unitPriceInTransactionCurrency, priceDifferencePercent);
                    String currentDescription = transaction.getDescription() != null ? transaction.getDescription() : "";
                    transaction.setDescription(currentDescription + auditNote);
                }
            }

            BigDecimal itemUsdAmount;
            if ("USD".equals(currency.getCode())) {
                itemUsdAmount = unitPriceInTransactionCurrency.multiply(BigDecimal.valueOf(quantity));
            } else {
                itemUsdAmount = unitPriceInTransactionCurrency
                        .divide(exchangeRate, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(quantity));
            }

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);
            transactionItem.setUnitPrice(itemUsdAmount.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP));
            transactionItem.setTotalPrice(itemUsdAmount);

            transactionItems.add(transactionItem);
            totalUsdAmount = totalUsdAmount.add(itemUsdAmount);
        }

        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.subtract(feeBalanceAmount));
        
        transaction.setItems(transactionItems);
    }

    /**
     * RETURN_PAYMENT transaction type uchun logika
     */
    private void calculateReturnPaymentTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        if (dto.getOriginalAmount() == null || dto.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for RETURN_PAYMENT transaction");
        }

        BigDecimal exchangeRate = transactionHelperService.getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = transactionHelperService.getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());
        BigDecimal originalAmount = dto.getOriginalAmount();

        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    /**
     * PURCHASE transaction type uchun logika
     */
    private void calculatePurchaseTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Items are required for PURCHASE transaction");
        }

        BigDecimal exchangeRate = transactionHelperService.getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = transactionHelperService.getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            BigDecimal basePriceInTransactionCurrency = product.getPriceUsd().multiply(exchangeRate);
            BigDecimal unitPriceInTransactionCurrency = item.getUnitPrice() != null 
                    ? item.getUnitPrice() 
                    : basePriceInTransactionCurrency;
            
            if (item.getUnitPrice() != null) {
                BigDecimal priceDifference = unitPriceInTransactionCurrency.subtract(basePriceInTransactionCurrency);
                BigDecimal priceDifferencePercent = basePriceInTransactionCurrency.compareTo(BigDecimal.ZERO) > 0
                        ? priceDifference.divide(basePriceInTransactionCurrency, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                
                if (priceDifferencePercent.abs().compareTo(BigDecimal.valueOf(5)) > 0) {
                    String auditNote = String.format(" [AUDIT: Custom price used. Base: %s, Actual: %s, Difference: %.2f%%]",
                            basePriceInTransactionCurrency, unitPriceInTransactionCurrency, priceDifferencePercent);
                    String currentDescription = transaction.getDescription() != null ? transaction.getDescription() : "";
                    transaction.setDescription(currentDescription + auditNote);
                }
            }

            BigDecimal itemUsdAmount;
            if ("USD".equals(currency.getCode())) {
                itemUsdAmount = unitPriceInTransactionCurrency.multiply(BigDecimal.valueOf(quantity));
            } else {
                itemUsdAmount = unitPriceInTransactionCurrency
                        .divide(exchangeRate, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(quantity));
            }

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);
            transactionItem.setUnitPrice(itemUsdAmount.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP));
            transactionItem.setTotalPrice(itemUsdAmount);

            transactionItems.add(transactionItem);
            totalUsdAmount = totalUsdAmount.add(itemUsdAmount);
        }

        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.subtract(feeBalanceAmount));
        
        transaction.setItems(transactionItems);
    }

    /**
     * TRANSFER transaction type uchun logika
     */
    private void calculateTransferTransaction(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        if (dto.getReceiverClientId() == null) {
            throw new BadRequestException("Receiver client ID is required for TRANSFER transaction");
        }
        
        // Receiver client mavjudligini tekshirish
        Client receiverClient = clientRepository.findById(dto.getReceiverClientId())
                .orElseThrow(() -> new EntityNotFoundException("Receiver client not found with id: " + dto.getReceiverClientId()));
        
        if (dto.getOriginalAmount() == null || dto.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for TRANSFER transaction");
        }

        BigDecimal exchangeRate = transactionHelperService.getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = transactionHelperService.getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());
        
        // Receiver exchange rate - DTO dan kelmasa DB dan olinadi
        BigDecimal receiverExchangeRate;
        if (dto.getReceiverExchangeRate() != null && dto.getReceiverExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            receiverExchangeRate = dto.getReceiverExchangeRate();
        } else {
            // DB dan receiver client ning balance currency uchun rate olish
            Currency receiverCurrency = receiverClient.getBalanceCurrency();
            if ("USD".equals(receiverCurrency.getCode())) {
                receiverExchangeRate = BigDecimal.ONE;
            } else {
                ExchangeRate rate = exchangeRateRepository
                        .findFirstByCurrencyOrderByCreatedAtDesc(receiverCurrency)
                        .orElseThrow(() -> new BadRequestException("Please provide receiver exchange rate for currency: " + receiverCurrency.getCode()));
                receiverExchangeRate = rate.getRate();
            }
            transactionHelperService.validateExchangeRate(receiverExchangeRate, receiverCurrency);
        }

        BigDecimal originalAmount = dto.getOriginalAmount();

        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        // Transaction ma'lumotlarini to'ldirish
        transaction.setReceiverClient(receiverClient);
        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setReceiverExchangeRate(receiverExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    // ==================== RECALCULATE METHODS ====================

    /**
     * SALE transaction ni qayta hisoblash
     */
    private void recalculateSaleTransaction(Transaction transaction, TransactionUpdateDTO dto, 
                                           Client client, Currency currency, 
                                           BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        List<TransactionItemCreateDTO> items;
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            items = dto.getItems();
        } else if (transaction.getItems() != null && !transaction.getItems().isEmpty()) {
            items = transaction.getItems().stream()
                    .map(item -> {
                        TransactionItemCreateDTO itemDto = new TransactionItemCreateDTO();
                        itemDto.setProductId(item.getProduct().getId());
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setUnitPrice(item.getUnitPrice().multiply(exchangeRate));
                        return itemDto;
                    })
                    .toList();
        } else {
            throw new BadRequestException("Items are required for SALE transaction");
        }

        if (items.isEmpty()) {
            throw new BadRequestException("Items are required for SALE transaction");
        }

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            BigDecimal basePriceInTransactionCurrency = product.getPriceUsd().multiply(exchangeRate);
            BigDecimal unitPriceInTransactionCurrency = item.getUnitPrice() != null 
                    ? item.getUnitPrice() 
                    : basePriceInTransactionCurrency;

            BigDecimal itemUsdAmount;
            if ("USD".equals(currency.getCode())) {
                itemUsdAmount = unitPriceInTransactionCurrency.multiply(BigDecimal.valueOf(quantity));
            } else {
                itemUsdAmount = unitPriceInTransactionCurrency
                        .divide(exchangeRate, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(quantity));
            }

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);
            transactionItem.setUnitPrice(itemUsdAmount.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP));
            transactionItem.setTotalPrice(itemUsdAmount);

            transactionItems.add(transactionItem);
            totalUsdAmount = totalUsdAmount.add(itemUsdAmount);
        }

        BigDecimal feeAmount = dto.getFeeAmount() != null ? dto.getFeeAmount() : transaction.getFeeAmount();
        if (feeAmount == null) {
            feeAmount = BigDecimal.ZERO;
        }
        transaction.setFeeAmount(feeAmount);

        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.add(feeBalanceAmount));
        
        transaction.setItems(transactionItems);
    }

    /**
     * PAYMENT transaction ni qayta hisoblash
     */
    private void recalculatePaymentTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                              Client client, Currency currency,
                                              BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        BigDecimal originalAmount = dto.getOriginalAmount() != null 
                ? dto.getOriginalAmount() 
                : transaction.getOriginalAmount();

        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for PAYMENT transaction");
        }

        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        BigDecimal feeAmount = dto.getFeeAmount() != null ? dto.getFeeAmount() : transaction.getFeeAmount();
        if (feeAmount == null) {
            feeAmount = BigDecimal.ZERO;
        }
        transaction.setFeeAmount(feeAmount);

        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    /**
     * RETURN transaction ni qayta hisoblash (SALE bilan bir xil)
     */
    private void recalculateReturnTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                             Client client, Currency currency,
                                             BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        recalculateSaleTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
    }

    /**
     * RETURN_PAYMENT transaction ni qayta hisoblash (PAYMENT bilan bir xil)
     */
    private void recalculateReturnPaymentTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                                    Client client, Currency currency,
                                                    BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        recalculatePaymentTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
    }

    /**
     * PURCHASE transaction ni qayta hisoblash (SALE bilan bir xil)
     */
    private void recalculatePurchaseTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                               Client client, Currency currency,
                                               BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        recalculateSaleTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
    }

    /**
     * TRANSFER transaction ni qayta hisoblash
     */
    private void recalculateTransferTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                              Client client, Currency currency,
                                              BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        BigDecimal originalAmount = dto.getOriginalAmount() != null 
                ? dto.getOriginalAmount() 
                : transaction.getOriginalAmount();

        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for TRANSFER transaction");
        }

        BigDecimal receiverExchangeRate = dto.getReceiverExchangeRate() != null 
                ? dto.getReceiverExchangeRate() 
                : transaction.getReceiverExchangeRate();

        if (receiverExchangeRate == null) {
            Client receiverClient = transaction.getReceiverClient();
            if (receiverClient == null) {
                throw new BadRequestException("Receiver client is required for TRANSFER transaction");
            }
            Currency receiverCurrency = receiverClient.getBalanceCurrency();
            if ("USD".equals(receiverCurrency.getCode())) {
                receiverExchangeRate = BigDecimal.ONE;
            } else {
                ExchangeRate rate = exchangeRateRepository
                        .findFirstByCurrencyOrderByCreatedAtDesc(receiverCurrency)
                        .orElseThrow(() -> new BadRequestException("Please provide receiver exchange rate for currency: " + receiverCurrency.getCode()));
                receiverExchangeRate = rate.getRate();
            }
            transactionHelperService.validateExchangeRate(receiverExchangeRate, receiverCurrency);
        }

        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        BigDecimal feeAmount = dto.getFeeAmount() != null ? dto.getFeeAmount() : transaction.getFeeAmount();
        if (feeAmount == null) {
            feeAmount = BigDecimal.ZERO;
        }
        transaction.setFeeAmount(feeAmount);

        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setReceiverExchangeRate(receiverExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }
}

