package uz.qarzdorlar_ai.service.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.enums.TransactionStatus;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.exception.BadRequestException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.ExchangeRateMapper;
import uz.qarzdorlar_ai.mapper.TransactionMapper;
import uz.qarzdorlar_ai.model.*;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.TransactionItemCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionUpdateDTO;
import uz.qarzdorlar_ai.repository.*;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final CurrencyRepository currencyRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final EnumSet<Role> ALLOWED_ROLES = EnumSet.of(Role.ADMIN, Role.DEVELOPER, Role.STAFF, Role.STAFF_PLUS);
    private final TransactionHelperService transactionHelperService;
    private final TransactionCalculationService transactionCalculationService;
    private final ExchangeRateRepository exchangeRateRepository;


    @Override
    @Transactional
    public TransactionDTO createTransaction(TransactionCreateDTO dto, User staffUser) {
        // Asosiy validatsiyalar va entitylarni olish
        TransactionType type = dto.getType();

        // Client mavjudligini tekshirish
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found with id: " + dto.getClientId())
                );

        // Currency mavjudligini tekshirish
        Currency currency = currencyRepository.findById(dto.getCurrencyId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id: " + dto.getCurrencyId())
                );

        // User mavjudligini va huquqini tekshirish
        User user = userRepository.findByUsername(staffUser.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + staffUser.getId())
                );

        Role role = user.getRole();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        // Transaction obyektini yaratish
        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCurrency(currency);
        transaction.setDescription(dto.getDescription());
        
        // createdAt - agar DTO dan kelmasa, current date ishlatiladi (AbsDateEntity @PrePersist tufayli)
        // Agar DTO dan kelgan bo'lsa, o'sha sanani ishlatamiz (migratsiya uchun)
        if (dto.getCreatedAt() != null) {
            transaction.setCreatedAt(dto.getCreatedAt());
        }
        // Aks holda AbsDateEntity.onCreate() avtomatik current date ni qo'yadi

        // Fee amount - agar berilgan bo'lsa, aks holda 0
        BigDecimal feeAmount = dto.getFeeAmount() != null ? dto.getFeeAmount() : BigDecimal.ZERO;
        transaction.setFeeAmount(feeAmount);

        // Har bir transaction type uchun alohida logika
        transactionCalculationService.calculateTransaction(dto, transaction, client, currency);

        // Transactionni saqlash
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Client balance yangilash
        transactionHelperService.updateClientBalance(savedTransaction);

        return transactionMapper.toDTO(savedTransaction);
    }

    // ==================== DELETED METHODS ====================
    // Helper metodlar endi TransactionHelperService da
    // Calculation metodlar endi TransactionCalculationService da
    // =========================================================

    /**
     * @deprecated Bu metod TransactionHelperService ga ko'chirildi
     */
    @Deprecated
    private BigDecimal getExchangeRate(BigDecimal dtoRate, Currency currency) {
        BigDecimal rate;
        
        // Agar DTO dan rate kelgan bo'lsa, uni ishlatamiz
        if (dtoRate != null && dtoRate.compareTo(BigDecimal.ZERO) > 0) {
            rate = dtoRate;
        } else if ("USD".equals(currency.getCode())) {
            // Agar USD bo'lsa, 1.0 qaytaramiz
            rate = BigDecimal.ONE;
        } else {
            // DB dan eng oxirgi rate ni olish
            ExchangeRate exchangeRate = exchangeRateRepository
                    .findFirstByCurrencyOrderByCreatedAtDesc(currency)
                    .orElseThrow(() -> new BadRequestException("Please provide exchange rate for currency: " + currency.getCode()));
            rate = exchangeRate.getRate();
        }

        // Sanity check - kurs mantiqiy chegarada bo'lishi kerak
        validateExchangeRate(rate, currency);
        
        return rate;
    }

    /**
     * Exchange rate validation - sanity check
     * Kurslar mantiqiy chegarada bo'lishi kerak (typo va xatoliklardan himoya qilish)
     */
    private void validateExchangeRate(BigDecimal rate, Currency currency) {
        String code = currency.getCode();
        
        // USD uchun 1.0 bo'lishi kerak
        if ("USD".equals(code)) {
            if (rate.compareTo(BigDecimal.ONE) != 0) {
                throw new BadRequestException("Exchange rate for USD must be 1.0, but received: " + rate);
            }
            return;
        }
        
        // UZS uchun kurs juda katta bo'lishi kerak (odatda 10000-15000 orasida)
        if ("UZS".equals(code)) {
            if (rate.compareTo(BigDecimal.valueOf(1000)) < 0 || rate.compareTo(BigDecimal.valueOf(50000)) > 0) {
                throw new BadRequestException(
                    String.format("Exchange rate for UZS seems incorrect. Expected range: 1000-50000, but received: %s. " +
                            "Please verify the rate to prevent data corruption.", rate)
                );
            }
            return;
        }
        
        // AED uchun kurs kichik bo'lishi kerak (odatda 3.5-4.0 orasida)
        if ("AED".equals(code)) {
            if (rate.compareTo(BigDecimal.valueOf(1.0)) < 0 || rate.compareTo(BigDecimal.valueOf(10.0)) > 0) {
                throw new BadRequestException(
                    String.format("Exchange rate for AED seems incorrect. Expected range: 1.0-10.0, but received: %s. " +
                            "Please verify the rate to prevent data corruption.", rate)
                );
            }
            return;
        }
        
        // Boshqa valyutalar uchun umumiy tekshiruv
        // Kurs 0 dan katta va 100000 dan kichik bo'lishi kerak
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.valueOf(100000)) > 0) {
            throw new BadRequestException(
                String.format("Exchange rate for %s seems incorrect. Received: %s. " +
                        "Please verify the rate to prevent data corruption.", code, rate)
            );
        }
    }

    /**
     * Client exchange rate olish uchun helper metod
     * Priority: DTO -> DB -> Xatolik
     * 
     * @param dtoRate DTO dan kelgan client exchange rate
     * @param clientCurrency Client ning balance currency
     * @return Client exchange rate
     */
    private BigDecimal getClientExchangeRate(BigDecimal dtoRate, Currency clientCurrency) {
        BigDecimal rate;
        
        // Agar DTO dan rate kelgan bo'lsa, uni ishlatamiz
        if (dtoRate != null && dtoRate.compareTo(BigDecimal.ZERO) > 0) {
            rate = dtoRate;
        } else if ("USD".equals(clientCurrency.getCode())) {
            // Agar USD bo'lsa, 1.0 qaytaramiz
            rate = BigDecimal.ONE;
        } else {
            // DB dan eng oxirgi rate ni olish
            ExchangeRate exchangeRate = exchangeRateRepository
                    .findFirstByCurrencyOrderByCreatedAtDesc(clientCurrency)
                    .orElseThrow(() -> new BadRequestException("Please provide client exchange rate for currency: " + clientCurrency.getCode()));
            rate = exchangeRate.getRate();
        }

        // Sanity check - kurs mantiqiy chegarada bo'lishi kerak
        validateExchangeRate(rate, clientCurrency);
        
        return rate;
    }

    /**
     * SALE transaction type uchun logika
     * Mijozdan tovar sotiladi, mijozning qarzi ko'payadi (currentBalance kamayadi)
     */
    private void handleTransactionSale(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        // Items majburiy - savdo qilish uchun mahsulotlar kerak
        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Items are required for SALE transaction");
        }

        // Exchange ratelarni olish
        BigDecimal exchangeRate = getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        // Har bir mahsulot uchun hisob-kitob
        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            // Product mavjudligini tekshirish
            // Product ni olish - soft delete bo'lsa ham o'chirilgan mahsulotlarni ko'rish uchun
            // @SQLRestriction tufayli o'chirilgan mahsulotlar ko'rinmaydi, lekin eski tranzaksiyalar uchun
            // findById ishlatamiz (bu @SQLRestriction ni e'tiborsiz qoldiradi)
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            // Agar DTO dan narx kelsa, u Transaction valyutasida (masalan, so'mda)
            // Bo'lmasa, mahsulotning bazadagi USD narxini olamiz
            BigDecimal basePriceInTransactionCurrency = product.getPriceUsd().multiply(exchangeRate);
            BigDecimal unitPriceInTransactionCurrency = item.getUnitPrice() != null 
                    ? item.getUnitPrice() 
                    : basePriceInTransactionCurrency;
            
            // Custom price audit - agar narx bazadagi narxdan farq qilsa, log qilish
            // Bu discount yoki markup ni kuzatish uchun
            if (item.getUnitPrice() != null) {
                BigDecimal priceDifference = unitPriceInTransactionCurrency.subtract(basePriceInTransactionCurrency);
                BigDecimal priceDifferencePercent = basePriceInTransactionCurrency.compareTo(BigDecimal.ZERO) > 0
                        ? priceDifference.divide(basePriceInTransactionCurrency, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                
                // Agar narx 5% dan ko'p farq qilsa, description ga qo'shamiz (audit uchun)
                if (priceDifferencePercent.abs().compareTo(BigDecimal.valueOf(5)) > 0) {
                    String auditNote = String.format(" [AUDIT: Custom price used. Base: %s, Actual: %s, Difference: %.2f%%]",
                            basePriceInTransactionCurrency, unitPriceInTransactionCurrency, priceDifferencePercent);
                    String currentDescription = transaction.getDescription() != null ? transaction.getDescription() : "";
                    transaction.setDescription(currentDescription + auditNote);
                }
            }

            // Transaction valyutasidagi narxni USD ga o'girish
            // Formula: (Narx / ExchangeRate) * Quantity = USD Amount
            BigDecimal itemUsdAmount;
            if ("USD".equals(currency.getCode())) {
                // Agar transaction USD da bo'lsa, to'g'ridan-to'g'ri ko'paytiramiz
                itemUsdAmount = unitPriceInTransactionCurrency.multiply(BigDecimal.valueOf(quantity));
            } else {
                // Boshqa valyutada bo'lsa, avval USD ga o'giramiz
                itemUsdAmount = unitPriceInTransactionCurrency
                        .divide(exchangeRate, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(quantity));
            }

            // Mijoz valyutasidagi qiymatni hisoblash (balanceAmount uchun)
            // Formula: USD Amount * Client Exchange Rate = Balance Amount
            BigDecimal itemBalanceAmount = itemUsdAmount.multiply(clientExchangeRate);

            // TransactionItem yaratish va saqlash
            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);
            transactionItem.setUnitPrice(itemUsdAmount.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP)); // 1 dona USD narxi
            transactionItem.setTotalPrice(itemUsdAmount); // Jami USD narxi

            transactionItems.add(transactionItem);
            totalUsdAmount = totalUsdAmount.add(itemUsdAmount);
        }

        // Fee amount ni hisobga olish (agar berilgan bo'lsa)
        BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal feeAmountInUsd = BigDecimal.ZERO;
        
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Fee amount transaction valyutasida, uni USD ga o'giramiz
            if ("USD".equals(currency.getCode())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            }
        }

        // Transaction ma'lumotlarini to'ldirish
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        // Balance amount - mijoz valyutasidagi jami summa
        // MUHIM: SALE holatida fee qarzga QO'SHILADI (qarz ko'payadi), ayirilmaydi
        // Fee - bu qo'shimcha daromad yoki xarajat, u mijozning bo'ynidagi umumiy summani oshiradi
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.add(feeBalanceAmount)); // QO'SHILADI, ayirilmaydi!
        
        transaction.setItems(transactionItems);
    }

    /**
     * PAYMENT transaction type uchun logika
     * Mijoz to'lov qiladi, mijozning qarzi kamayadi (currentBalance oshadi)
     */
    private void handleTransactionPayment(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        // Original amount majburiy - to'lov qilish uchun summa kerak
        if (dto.getOriginalAmount() == null || dto.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for PAYMENT transaction");
        }

        // Exchange ratelarni olish
        BigDecimal exchangeRate = getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        // Original amount transaction valyutasida (masalan, so'mda)
        BigDecimal originalAmount = dto.getOriginalAmount();

        // USD ga o'girish
        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        // Fee amount ni hisobga olish
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
        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        // Balance amount - mijoz valyutasidagi summa (fee ni minus qilib)
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    /**
     * RETURN transaction type uchun logika
     * Mijoz tovar qaytaradi, mijozning qarzi kamayadi (currentBalance oshadi)
     * SALE bilan bir xil tuzilish, lekin balansga ta'siri teskari
     */
    private void handleTransactionReturn(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        // Items majburiy - qaytarilayotgan mahsulotlar kerak
        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Items are required for RETURN transaction");
        }

        // Exchange ratelarni olish
        BigDecimal exchangeRate = getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        // Har bir mahsulot uchun hisob-kitob (SALE bilan bir xil)
        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            // Product ni olish - soft delete bo'lsa ham o'chirilgan mahsulotlarni ko'rish uchun
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            // Custom price audit - agar narx bazadagi narxdan farq qilsa, log qilish
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
                
                // Agar narx 5% dan ko'p farq qilsa, description ga qo'shamiz (audit uchun)
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

            BigDecimal itemBalanceAmount = itemUsdAmount.multiply(clientExchangeRate);

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setTransaction(transaction);
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);
            transactionItem.setUnitPrice(itemUsdAmount.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP));
            transactionItem.setTotalPrice(itemUsdAmount);

            transactionItems.add(transactionItem);
            totalUsdAmount = totalUsdAmount.add(itemUsdAmount);
        }

        // Fee amount ni hisobga olish
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
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        // Balance amount - mijoz valyutasidagi jami summa (fee ni minus qilib)
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.subtract(feeBalanceAmount));
        
        transaction.setItems(transactionItems);
    }

    /**
     * RETURN_PAYMENT transaction type uchun logika
     * To'lov qaytariladi (masalan, courier pul bermasa, o'zimizga qaytaradi)
     * Mijozning qarzi ko'payadi (currentBalance kamayadi)
     */
    private void handleTransactionReturnPayment(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        // Original amount majburiy - qaytarilayotgan summa kerak
        if (dto.getOriginalAmount() == null || dto.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for RETURN_PAYMENT transaction");
        }

        // Exchange ratelarni olish
        BigDecimal exchangeRate = getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        BigDecimal originalAmount = dto.getOriginalAmount();

        // USD ga o'girish
        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        // Fee amount ni hisobga olish
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
        transaction.setOriginalAmount(originalAmount);
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(usdAmount);
        
        // Balance amount - mijoz valyutasidagi summa (fee ni minus qilib)
        // RETURN_PAYMENT da qarz ko'payadi, shuning uchun minus qilamiz
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    /**
     * PURCHASE transaction type uchun logika
     * Supplierdan tovar sotib olinadi, bizning qarzimiz ko'payadi (supplier balansi oshadi)
     */
    private void handleTransactionPurchase(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        // Items majburiy - sotib olinayotgan mahsulotlar kerak
        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Items are required for PURCHASE transaction");
        }

        // Exchange ratelarni olish
        BigDecimal exchangeRate = getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalUsdAmount = BigDecimal.ZERO;

        // Har bir mahsulot uchun hisob-kitob
        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero for product id: " + productId);
            }

            // Product ni olish - soft delete bo'lsa ham o'chirilgan mahsulotlarni ko'rish uchun
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

            // Custom price audit - agar narx bazadagi narxdan farq qilsa, log qilish
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
                
                // Agar narx 5% dan ko'p farq qilsa, description ga qo'shamiz (audit uchun)
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

        // Fee amount ni hisobga olish
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
        transaction.setExchangeRate(exchangeRate);
        transaction.setClientExchangeRate(clientExchangeRate);
        transaction.setUsdAmount(totalUsdAmount);
        
        // Balance amount - supplier valyutasidagi jami summa (fee ni minus qilib)
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(totalBalanceAmount.subtract(feeBalanceAmount));
        
        transaction.setItems(transactionItems);
    }

    /**
     * TRANSFER transaction type uchun logika
     * Client1 dan Client2 ga pul o'tkaziladi
     * Client1 balansi kamayadi, Client2 balansi oshadi
     */
    private void handleTransactionTransfer(TransactionCreateDTO dto, Transaction transaction, Client client, Currency currency) {
        // Receiver client majburiy
        if (dto.getReceiverClientId() == null) {
            throw new BadRequestException("Receiver client ID is required for TRANSFER transaction");
        }

        // Receiver client mavjudligini tekshirish
        Client receiverClient = clientRepository.findById(dto.getReceiverClientId())
                .orElseThrow(() -> new EntityNotFoundException("Receiver client not found with id: " + dto.getReceiverClientId()));

        // Original amount majburiy
        if (dto.getOriginalAmount() == null || dto.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original amount is required and must be greater than zero for TRANSFER transaction");
        }

        // Exchange ratelarni olish
        BigDecimal exchangeRate = getExchangeRate(dto.getExchangeRate(), currency);
        BigDecimal clientExchangeRate = getClientExchangeRate(dto.getClientExchangeRate(), client.getBalanceCurrency());
        
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
        }

        BigDecimal originalAmount = dto.getOriginalAmount();

        // USD ga o'girish
        BigDecimal usdAmount;
        if ("USD".equals(currency.getCode())) {
            usdAmount = originalAmount;
        } else {
            usdAmount = originalAmount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
        }

        // Fee amount ni hisobga olish
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
        
        // Balance amount - client valyutasidagi summa (fee ni minus qilib)
        // Client1 dan ayiriladi
        BigDecimal balanceAmount = usdAmount.multiply(clientExchangeRate);
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(clientExchangeRate);
        transaction.setBalanceAmount(balanceAmount.subtract(feeBalanceAmount));
    }

    /**
     * Client balance yangilash
     * Har bir transaction type uchun balansga ta'sir qiladi
     * 
     * MUHIM: Race condition oldini olish uchun SQL darajasida atomic operation ishlatiladi
     * Bu bir vaqtning o'zida bir nechta tranzaksiya kelganda balanslarni to'g'ri yangilashni ta'minlaydi
     * 
     * Mantiq: currentBalance manfiy bo'lsa - mijoz bizdan qarz olgan (qarzdor)
     *         currentBalance musbat bo'lsa - biz mijozdan qarz olganmiz (qarzimiz bor)
     */
    private void updateClientBalance(Transaction transaction) {
        Client client = transaction.getClient();
        BigDecimal balanceAmount = transaction.getBalanceAmount();

        // Transaction type ga qarab balansni yangilash
        // SQL darajasida atomic operation - race condition oldini oladi
        switch (transaction.getType()) {
            case SALE, RETURN_PAYMENT, PURCHASE -> {
                // Qarz ko'payadi: currentBalance = currentBalance - balanceAmount
                // SALE: mijozdan tovar sotiladi, mijozning qarzi ko'payadi (balans kamayadi)
                // RETURN_PAYMENT: to'lov qaytariladi, qarz ko'payadi
                // PURCHASE: supplierdan tovar sotib olinadi, bizning qarzimiz ko'payadi (supplier balansi oshadi)
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount.negate());
                if (updated == 0) {
                    throw new BadRequestException("Failed to update client balance. Client may have been deleted or modified.");
                }
            }
            case PAYMENT, RETURN -> {
                // Qarz kamayadi: currentBalance = currentBalance + balanceAmount
                // PAYMENT: mijoz to'lov qiladi, qarzi kamayadi (balans oshadi)
                // RETURN: tovar qaytariladi, qarz kamayadi
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount);
                if (updated == 0) {
                    throw new BadRequestException("Failed to update client balance. Client may have been deleted or modified.");
                }
            }
            case TRANSFER -> {
                // Client1 dan pul ayiriladi (qarzi ko'payadi yoki qarzimiz kamayadi)
                int updated1 = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount.negate());
                if (updated1 == 0) {
                    throw new BadRequestException("Failed to update sender client balance. Client may have been deleted or modified.");
                }

                // Client2 ga pul qo'shiladi (qarzi kamayadi yoki qarzimiz ko'payadi)
                Client receiverClient = transaction.getReceiverClient();
                if (receiverClient != null) {
                    // Receiver uchun balance amount hisoblash (receiver valyutasida)
                    BigDecimal receiverBalanceAmount = transaction.getUsdAmount()
                            .multiply(transaction.getReceiverExchangeRate());
                    
                    // Receiver ga pul qo'shiladi (qarzi kamayadi)
                    int updated2 = clientRepository.updateBalanceAtomic(receiverClient.getId(), receiverBalanceAmount);
                    if (updated2 == 0) {
                        throw new BadRequestException("Failed to update receiver client balance. Client may have been deleted or modified.");
                    }
                }
            }
        }
    }


    @Override
    @Transactional
    public TransactionDTO updateTransaction(Long id, TransactionUpdateDTO dto, User staffUser) {
        // Transaction ni topish
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));

        // User huquqini tekshirish
        User user = userRepository.findByUsername(staffUser.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + staffUser.getId()));

        Role role = user.getRole();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        // Eski balansni qaytarish (revert) - avval eski transaction ning ta'sirini bekor qilamiz
        transactionHelperService.revertClientBalance(transaction);

        // Yangi ma'lumotlarni o'rnatish
        boolean needsRecalculation = false;

        // Client o'zgartirilgan bo'lsa
        if (dto.getClientId() != null && !dto.getClientId().equals(transaction.getClient().getId())) {
            Client newClient = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + dto.getClientId()));
            transaction.setClient(newClient);
            needsRecalculation = true;
        }

        // Receiver client o'zgartirilgan bo'lsa (TRANSFER uchun)
        if (dto.getReceiverClientId() != null) {
            if (transaction.getType() != TransactionType.TRANSFER) {
                throw new BadRequestException("Receiver client can only be set for TRANSFER transactions");
            }
            Client newReceiverClient = clientRepository.findById(dto.getReceiverClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Receiver client not found with id: " + dto.getReceiverClientId()));
            transaction.setReceiverClient(newReceiverClient);
            needsRecalculation = true;
        }

        // Currency o'zgartirilgan bo'lsa
        if (dto.getCurrencyId() != null && !dto.getCurrencyId().equals(transaction.getCurrency().getId())) {
            Currency newCurrency = currencyRepository.findById(dto.getCurrencyId())
                    .orElseThrow(() -> new EntityNotFoundException("Currency not found with id: " + dto.getCurrencyId()));
            transaction.setCurrency(newCurrency);
            needsRecalculation = true;
        }

        // Exchange rate o'zgartirilgan bo'lsa
        if (dto.getExchangeRate() != null) {
            transaction.setExchangeRate(dto.getExchangeRate());
            needsRecalculation = true;
        }

        // Client exchange rate o'zgartirilgan bo'lsa
        if (dto.getClientExchangeRate() != null) {
            transaction.setClientExchangeRate(dto.getClientExchangeRate());
            needsRecalculation = true;
        }

        // Receiver exchange rate o'zgartirilgan bo'lsa (TRANSFER uchun)
        if (dto.getReceiverExchangeRate() != null) {
            if (transaction.getType() != TransactionType.TRANSFER) {
                throw new BadRequestException("Receiver exchange rate can only be set for TRANSFER transactions");
            }
            transaction.setReceiverExchangeRate(dto.getReceiverExchangeRate());
            needsRecalculation = true;
        }

        // Original amount o'zgartirilgan bo'lsa
        if (dto.getOriginalAmount() != null) {
            transaction.setOriginalAmount(dto.getOriginalAmount());
            needsRecalculation = true;
        }

        // Fee amount o'zgartirilgan bo'lsa
        if (dto.getFeeAmount() != null) {
            transaction.setFeeAmount(dto.getFeeAmount());
            needsRecalculation = true;
        }

        // Description o'zgartirilgan bo'lsa
        if (dto.getDescription() != null) {
            transaction.setDescription(dto.getDescription());
        }

        // Items o'zgartirilgan bo'lsa (SALE, RETURN, PURCHASE uchun)
        if (dto.getItems() != null) {
            TransactionType type = transaction.getType();
            if (type != TransactionType.SALE && type != TransactionType.RETURN && type != TransactionType.PURCHASE) {
                throw new BadRequestException("Items can only be updated for SALE, RETURN, or PURCHASE transactions");
            }
            // Eski items larni o'chirish
            if (transaction.getItems() != null) {
                transaction.getItems().clear();
            }
            needsRecalculation = true;
        }

        // Status o'zgartirilgan bo'lsa
        if (dto.getStatus() != null) {
            transaction.setStatus(dto.getStatus());
        }

        // Agar qayta hisoblash kerak bo'lsa, qayta hisoblaymiz
        if (needsRecalculation) {
            transactionCalculationService.recalculateTransaction(transaction, dto);
        }

        // Transactionni saqlash
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Yangi balansni qo'llash
        transactionHelperService.updateClientBalance(savedTransaction);

        return transactionMapper.toDTO(savedTransaction);
    }

    /**
     * Eski transaction ning balansga ta'sirini qaytarish (revert)
     * Update qilishdan oldin eski balansni qaytarish uchun
     */
    private void revertClientBalance(Transaction transaction) {
        Client client = transaction.getClient();
        BigDecimal balanceAmount = transaction.getBalanceAmount();

        // Transaction type ga qarab balansni teskari qilish
        switch (transaction.getType()) {
            case SALE, RETURN_PAYMENT, PURCHASE -> {
                // Eski qarzni qaytarish: currentBalance = currentBalance + balanceAmount
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount);
                if (updated == 0) {
                    throw new BadRequestException("Failed to revert client balance. Client may have been deleted or modified.");
                }
            }
            case PAYMENT, RETURN -> {
                // Eski to'lovni qaytarish: currentBalance = currentBalance - balanceAmount
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount.negate());
                if (updated == 0) {
                    throw new BadRequestException("Failed to revert client balance. Client may have been deleted or modified.");
                }
            }
            case TRANSFER -> {
                // Client1 ga qaytarish
                int updated1 = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount);
                if (updated1 == 0) {
                    throw new BadRequestException("Failed to revert sender client balance. Client may have been deleted or modified.");
                }

                // Client2 dan ayirish
                Client receiverClient = transaction.getReceiverClient();
                if (receiverClient != null) {
                    // Receiver exchange rate ni olish
                    BigDecimal receiverExchangeRate = transaction.getReceiverExchangeRate();
                    if (receiverExchangeRate == null) {
                        // Agar receiverExchangeRate null bo'lsa, receiver client ning balance currency dan olamiz
                        Currency receiverCurrency = receiverClient.getBalanceCurrency();
                        if ("USD".equals(receiverCurrency.getCode())) {
                            receiverExchangeRate = BigDecimal.ONE;
                        } else {
                            ExchangeRate rate = exchangeRateRepository
                                    .findFirstByCurrencyOrderByCreatedAtDesc(receiverCurrency)
                                    .orElseThrow(() -> new BadRequestException("Receiver exchange rate not found for currency: " + receiverCurrency.getCode()));
                            receiverExchangeRate = rate.getRate();
                        }
                    }
                    
                    BigDecimal receiverBalanceAmount = transaction.getUsdAmount()
                            .multiply(receiverExchangeRate);
                    
                    int updated2 = clientRepository.updateBalanceAtomic(receiverClient.getId(), receiverBalanceAmount.negate());
                    if (updated2 == 0) {
                        throw new BadRequestException("Failed to revert receiver client balance. Client may have been deleted or modified.");
                    }
                }
            }
        }
    }

    /**
     * Transaction ni qayta hisoblash
     * Yangi ma'lumotlar bilan balanceAmount va usdAmount ni qayta hisoblaydi
     */
    private void recalculateTransaction(Transaction transaction, TransactionUpdateDTO dto) {
        Client client = transaction.getClient();
        Currency currency = transaction.getCurrency();
        TransactionType type = transaction.getType();

        // Exchange ratelarni olish (yangi yoki eski)
        BigDecimal exchangeRate = dto.getExchangeRate() != null 
                ? dto.getExchangeRate() 
                : transaction.getExchangeRate();
        
        BigDecimal clientExchangeRate = dto.getClientExchangeRate() != null 
                ? dto.getClientExchangeRate() 
                : transaction.getClientExchangeRate();

        // Validation - exchange ratelarni tekshirish
        validateExchangeRate(exchangeRate, currency);
        validateExchangeRate(clientExchangeRate, client.getBalanceCurrency());

        // Transaction type ga qarab qayta hisoblash
        switch (type) {
            case SALE -> recalculateSaleTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case PAYMENT -> recalculatePaymentTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case RETURN -> recalculateReturnTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case RETURN_PAYMENT -> recalculateReturnPaymentTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case PURCHASE -> recalculatePurchaseTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
            case TRANSFER -> recalculateTransferTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
        }
    }

    /**
     * SALE transaction ni qayta hisoblash
     */
    private void recalculateSaleTransaction(Transaction transaction, TransactionUpdateDTO dto, 
                                           Client client, Currency currency, 
                                           BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        // Items majburiy - agar DTO dan kelmasa, eski items lardan olamiz
        List<TransactionItemCreateDTO> items;
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            items = dto.getItems();
        } else if (transaction.getItems() != null && !transaction.getItems().isEmpty()) {
            // Eski items lardan DTO yaratish
            items = transaction.getItems().stream()
                    .map(item -> {
                        TransactionItemCreateDTO itemDto = new TransactionItemCreateDTO();
                        itemDto.setProductId(item.getProduct().getId());
                        itemDto.setQuantity(item.getQuantity());
                        // USD narxni transaction currency ga o'girish
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
        // RETURN SALE bilan bir xil, faqat balansga ta'siri teskari
        recalculateSaleTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
    }

    /**
     * RETURN_PAYMENT transaction ni qayta hisoblash (PAYMENT bilan bir xil)
     */
    private void recalculateReturnPaymentTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                                    Client client, Currency currency,
                                                    BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        // RETURN_PAYMENT PAYMENT bilan bir xil, faqat balansga ta'siri teskari
        recalculatePaymentTransaction(transaction, dto, client, currency, exchangeRate, clientExchangeRate);
    }

    /**
     * PURCHASE transaction ni qayta hisoblash (SALE bilan bir xil)
     */
    private void recalculatePurchaseTransaction(Transaction transaction, TransactionUpdateDTO dto,
                                               Client client, Currency currency,
                                               BigDecimal exchangeRate, BigDecimal clientExchangeRate) {
        // PURCHASE SALE bilan bir xil
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
            validateExchangeRate(receiverExchangeRate, receiverCurrency);
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

    @Override
    @Transactional
    public void deleteTransaction(Long id, User staffUser) {
        // Transaction ni topish
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));

        // User huquqini tekshirish
        User user = userRepository.findByUsername(staffUser.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + staffUser.getId()));

        Role role = user.getRole();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        // Eski balansni qaytarish (revert) - transaction ning ta'sirini bekor qilamiz
        // Bu muhim, chunki transaction o'chirilganda client balansi to'g'ri bo'lishi kerak
        transactionHelperService.revertClientBalance(transaction);

        // Transaction ni soft delete qilish
        // @SQLDelete annotation tufayli deleted = true bo'ladi
        transactionRepository.delete(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getByIdTransection(Long id) {

        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Transaction not found with id : " + id));

        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<TransactionDTO> getByAllTransection(Integer page, Integer size) {

        Sort sort = Sort.by(AbsDateEntity.Fields.createdAt);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Transaction> transactionPage = transactionRepository.findAll(pageRequest);

        return new PageDTO<>(transactionPage.getContent().stream().map(transactionMapper::toDTO).toList(), transactionPage);
    }
}
