package uz.qarzdorlar_ai.service.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.enums.TransactionStatus;
import uz.qarzdorlar_ai.exception.BadRequestException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.model.TransactionItem;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionItemCreateDTO;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.repository.ProductRepository;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionCalculationServiceImpl implements TransactionCalculationService {

    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;

    @Override
    public void calculateTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        switch (transaction.getType()) {
            case SALE -> calculateSaleTransaction(dto, transaction, client);
            case PURCHASE -> calculatePurchaseTransaction(dto, transaction, client);
            case RETURN -> calculateReturnTransaction(dto, transaction, client);

            case CASH_IN -> calculateCashInTransaction(dto, transaction, client);
            case CASH_OUT -> calculateCashOutTransaction(dto, transaction, client);

            case TRANSFER -> calculateTransferTransaction(dto, transaction, client);

            default -> throw new BadRequestException("Unsupported transaction type: " + transaction.getType());
        }


    }

    private void calculateSaleTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Sotuv tranzaksiyasi uchun mahsulotlar kiritilishi shart!");
        }

        BigDecimal rateToUsd = BigDecimal.ONE;
        if (!transaction.getTransactionCurrency().equals(CurrencyCode.USD)) {
            if (dto.getRateToUsd() == null)
                throw new BadRequestException("Transaksiya dolorda amalga oshirilmasa rate kiritsh majburiy");
            rateToUsd = dto.getRateToUsd(); // tr valyutasi usd bolmasa client kiritgan rate olinadi
        }

        BigDecimal clientRateToUsd = BigDecimal.ONE;
        if (!client.getCurrencyCode().equals(CurrencyCode.USD)) {
            if (dto.getClientRateToUsd() == null)
                throw new BadRequestException("Client dolorda qarzdor bolmasa rate kiritsh majburiy");
            clientRateToUsd = dto.getClientRateToUsd(); // clinet valyutasi dolor bolsa clinet kiritgan rate
        }

        List<TransactionItem> items = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO; // item larning umumiy summasi uchun
        for (TransactionItemCreateDTO item : dto.getItems()) {

            Long productId = item.getProductId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Product not found with id : " + productId)
                    );
            Integer quantity = item.getQuantity();
            BigDecimal oneItemPrice;
            if (item.getUnitPrice() != null) {
                oneItemPrice = item.getUnitPrice();
            } else {
                oneItemPrice = product.getPriceUsd().multiply(rateToUsd);
            }
            BigDecimal totalPrice = oneItemPrice.multiply(BigDecimal.valueOf(quantity));

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setProduct(product); // clinetga tanlangan product
            transactionItem.setQuantity(quantity); // soni
            transactionItem.setUnitPrice(oneItemPrice); // tr valyutasidagi product price
            transactionItem.setTotalPrice(totalPrice); // itemdani soniga kopaytirdim

            transactionItem.setTransaction(transaction);

            items.add(transactionItem); // entity qilib listga yig`amiz
            amount = amount.add(totalPrice); // item larni umumiy sumasini qoshamiz
        }
        BigDecimal usdAmount = amount.divide(rateToUsd, 4, RoundingMode.HALF_UP);

        transaction.setAmount(amount); // bu yerda tr paytida itemning umumiy summasi bu valyuta tr paytidagi boladi
        transaction.setRateToUsd(rateToUsd); // tr valyutasi USD nisbati
        transaction.setUsdAmount(usdAmount); // itemning dolordagi qiymadi
        transaction.setClientRateSnapshot(clientRateToUsd); // clinet valyutasining dolorga nisbati

        BigDecimal balanceEffect = usdAmount.multiply(clientRateToUsd);
        transaction.setBalanceEffect(balanceEffect.negate());

        transaction.setItems(items);
    }

    private void calculatePurchaseTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Purchase  tranzaksiyasi uchun mahsulotlar kiritilishi shart!");
        }

        BigDecimal rateToUsd = BigDecimal.ONE;
        if (!transaction.getTransactionCurrency().equals(CurrencyCode.USD)) {
            if (dto.getRateToUsd() == null)
                throw new BadRequestException("Puchase dolorda amalga oshirilmasa rate kiritsh majburiy");
            rateToUsd = dto.getRateToUsd(); // tr valyutasi usd bolmasa client kiritgan rate olinadi
        }

        BigDecimal clientRateToUsd = BigDecimal.ONE;
        if (!client.getCurrencyCode().equals(CurrencyCode.USD)) {
            if (dto.getClientRateToUsd() == null)
                throw new BadRequestException("Client dolorda qarzdor bolmasa rate kiritsh majburiy");
            clientRateToUsd = dto.getClientRateToUsd(); // clinet valyutasi dolor bolsa clinet kiritgan rate
        }

        List<TransactionItem> items = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO; // item larning umumiy summasi uchun
        for (TransactionItemCreateDTO item : dto.getItems()) {

            Long productId = item.getProductId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Product not found with id : " + productId)
                    );
            Integer quantity = item.getQuantity();
            BigDecimal oneItemPrice;
            if (item.getUnitPrice() != null) {
                oneItemPrice = item.getUnitPrice();
            } else {
                oneItemPrice = product.getPriceUsd().multiply(rateToUsd);
            }
            BigDecimal totalPrice = oneItemPrice.multiply(BigDecimal.valueOf(quantity));

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setProduct(product); // clinetga tanlangan product
            transactionItem.setQuantity(quantity); // soni
            transactionItem.setUnitPrice(oneItemPrice); // tr valyutasidagi product price
            transactionItem.setTotalPrice(totalPrice); // itemdani soniga kopaytirdim

            transactionItem.setTransaction(transaction);

            items.add(transactionItem); // entity qilib listga yig`amiz
            amount = amount.add(totalPrice); // item larni umumiy sumasini qoshamiz
        }
        BigDecimal usdAmount = amount.divide(rateToUsd, 4, RoundingMode.HALF_UP);

        transaction.setAmount(amount); // bu yerda tr paytida itemning umumiy summasi bu valyuta tr paytidagi boladi
        transaction.setRateToUsd(rateToUsd); // tr valyutasi USD nisbati
        transaction.setUsdAmount(usdAmount); // itemning dolordagi qiymadi
        transaction.setClientRateSnapshot(clientRateToUsd); // clinet valyutasining dolorga nisbati

        BigDecimal balanceEffect = usdAmount.multiply(clientRateToUsd);
        transaction.setBalanceEffect(balanceEffect);

        transaction.setItems(items);

    }

    private void calculateReturnTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Sotuv tranzaksiyasi uchun mahsulotlar kiritilishi shart!");
        }

        BigDecimal rateToUsd = BigDecimal.ONE;
        if (!transaction.getTransactionCurrency().equals(CurrencyCode.USD)) {
            if (dto.getRateToUsd() == null)
                throw new BadRequestException("Transaksiya dolorda amalga oshirilmasa rate kiritsh majburiy");
            rateToUsd = dto.getRateToUsd(); // tr valyutasi usd bolmasa client kiritgan rate olinadi
        }

        BigDecimal clientRateToUsd = BigDecimal.ONE;
        if (!client.getCurrencyCode().equals(CurrencyCode.USD)) {
            if (dto.getClientRateToUsd() == null)
                throw new BadRequestException("Client dolorda qarzdor bolmasa rate kiritsh majburiy");
            clientRateToUsd = dto.getClientRateToUsd(); // clinet valyutasi dolor bolsa clinet kiritgan rate
        }

        List<TransactionItem> items = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO; // item larning umumiy summasi uchun
        for (TransactionItemCreateDTO item : dto.getItems()) {

            Long productId = item.getProductId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Product not found with id : " + productId)
                    );
            Integer quantity = item.getQuantity();
            BigDecimal oneItemPrice;
            if (item.getUnitPrice() != null) {
                oneItemPrice = item.getUnitPrice();
            } else {
                oneItemPrice = product.getPriceUsd().multiply(rateToUsd);
            }
            BigDecimal totalPrice = oneItemPrice.multiply(BigDecimal.valueOf(quantity));

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setProduct(product); // clinetga tanlangan product
            transactionItem.setQuantity(quantity); // soni
            transactionItem.setUnitPrice(oneItemPrice); // tr valyutasidagi product price
            transactionItem.setTotalPrice(totalPrice); // itemdani soniga kopaytirdim

            items.add(transactionItem); // entity qilib listga yig`amiz
            amount = amount.add(totalPrice); // item larni umumiy sumasini qoshamiz
        }
        BigDecimal usdAmount = amount.divide(rateToUsd, 4, RoundingMode.HALF_UP);

        transaction.setAmount(amount); // bu yerda tr paytida itemning umumiy summasi bu valyuta tr paytidagi boladi
        transaction.setRateToUsd(rateToUsd); // tr valyutasi USD nisbati
        transaction.setUsdAmount(usdAmount); // itemning dolordagi qiymadi
        transaction.setClientRateSnapshot(clientRateToUsd); // clinet valyutasining dolorga nisbati

        BigDecimal balanceEffect = usdAmount.multiply(clientRateToUsd);
        transaction.setBalanceEffect(balanceEffect);

        transaction.setItems(items);

    }

    private void calculateCashInTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        if (dto.getItems() != null && dto.getItems().isEmpty()) {
            throw new BadRequestException("Cash-in tranzaksiyasi uchun mahsulot kirtilmaydi");
        }

        BigDecimal rateToUsd = BigDecimal.ONE;
        if (!transaction.getTransactionCurrency().equals(CurrencyCode.USD)) {
            if (dto.getRateToUsd() == null)
                throw new BadRequestException("Cash-in dolorda amalga oshirilmasa rate kiritsh majburiy");
            rateToUsd = dto.getRateToUsd(); // tr valyutasi usd bolmasa client kiritgan rate olinadi
        }

        BigDecimal clientRateToUsd = BigDecimal.ONE;
        if (!client.getCurrencyCode().equals(CurrencyCode.USD)) {
            if (dto.getClientRateToUsd() == null)
                throw new BadRequestException("Client dolorda qarzdor bolmasa rate kiritsh majburiy");
            clientRateToUsd = dto.getClientRateToUsd(); // clinet valyutasi dolor bolsa clinet kiritgan rate
        }

        BigDecimal amount = dto.getAmount();
        if (dto.getAmount() == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Cash-in paytida qancha pul olinayotgani 0 dan katta bo‘lishi shart"
            );
        }

        BigDecimal usdAmount = amount.divide(rateToUsd, 4, RoundingMode.HALF_UP);

        transaction.setRateToUsd(rateToUsd);
        transaction.setAmount(amount);
        transaction.setUsdAmount(usdAmount);
        transaction.setClientRateSnapshot(clientRateToUsd);
        BigDecimal balanceEffect = usdAmount.multiply(clientRateToUsd);
        transaction.setBalanceEffect(balanceEffect);


    }

    private void calculateCashOutTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            throw new BadRequestException("Cash-out tranzaksiyasi uchun mahsulot kirtilmaydi");
        }

        BigDecimal rateToUsd = BigDecimal.ONE;
        if (!transaction.getTransactionCurrency().equals(CurrencyCode.USD)) {
            if (dto.getRateToUsd() == null)
                throw new BadRequestException("Cash-out dolorda amalga oshirilmasa rate kiritsh majburiy");
            rateToUsd = dto.getRateToUsd(); // tr valyutasi usd bolmasa client kiritgan rate olinadi
        }

        BigDecimal clientRateToUsd = BigDecimal.ONE;
        if (!client.getCurrencyCode().equals(CurrencyCode.USD)) {
            if (dto.getClientRateToUsd() == null)
                throw new BadRequestException("Client dolorda qarzdor bolmasa rate kiritsh majburiy");
            clientRateToUsd = dto.getClientRateToUsd(); // clinet valyutasi dolor bolsa clinet kiritgan rate
        }

        BigDecimal amount = dto.getAmount();
        if (dto.getAmount() == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Cash-out paytida qancha pul olinayotgani 0 dan katta bo‘lishi shart"
            );
        }
        BigDecimal usdAmount = amount.divide(rateToUsd, 4, RoundingMode.HALF_UP);

        BigDecimal feeAmount = dto.getFeeAmount();

        BigDecimal clxBalance = usdAmount.subtract(feeAmount == null ? BigDecimal.ZERO : feeAmount);

        transaction.setRateToUsd(rateToUsd);

        transaction.setAmount(amount);
        transaction.setUsdAmount(usdAmount);
        transaction.setClientRateSnapshot(clientRateToUsd);

        BigDecimal balanceEffect = clxBalance.multiply(clientRateToUsd);

        transaction.setBalanceEffect(balanceEffect.negate());

    }

    private void calculateTransferTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {


        if (dto.getItems() != null && dto.getItems().isEmpty()) {
            throw new BadRequestException("Transfer tranzaksiyasi uchun mahsulot kirtilmaydi");
        }

        BigDecimal rateToUsd = BigDecimal.ONE;
        if (!transaction.getTransactionCurrency().equals(CurrencyCode.USD)) {
            if (dto.getRateToUsd() == null)
                throw new BadRequestException("Transfer dolorda amalga oshirilmasa rate kiritsh majburiy");
            rateToUsd = dto.getRateToUsd(); // tr valyutasi usd bolmasa client kiritgan rate olinadi
        }

        BigDecimal clientRateToUsd = BigDecimal.ONE;
        if (!client.getCurrencyCode().equals(CurrencyCode.USD)) {
            if (dto.getClientRateToUsd() == null)
                throw new BadRequestException("Client dolorda qarzdor bolmasa rate kiritsh majburiy");
            clientRateToUsd = dto.getClientRateToUsd(); // clinet valyutasi dolor bolsa clinet kiritgan rate
        }

        if (dto.getReceiverClientId() == null) {
            throw new BadRequestException("Transfer paytida Receiver bolishi shart");
        }

        Client receiverClient = clientRepository.findById(dto.getReceiverClientId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Receiver client not found with receiver id : " + dto.getReceiverClientId())
                );

        BigDecimal receiverRateUsd = BigDecimal.ONE;
        if (!receiverClient.getCurrencyCode().equals(CurrencyCode.USD)) {
            receiverRateUsd = dto.getReceiverRateToUsd();
        }

        BigDecimal amount = dto.getAmount();
        if (dto.getAmount() == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Transfer paytida qancha pul olinayotgani 0 dan katta bo‘lishi shart"
            );
        }

        transaction.setRateToUsd(rateToUsd);
        transaction.setAmount(amount);
        BigDecimal usdAmount = amount.divide(rateToUsd, 4, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);
        transaction.setClientRateSnapshot(clientRateToUsd);

        BigDecimal clientBalanceEffect = usdAmount.multiply(clientRateToUsd);
        transaction.setBalanceEffect(clientBalanceEffect);

        transaction.setReceiverClient(receiverClient);
        transaction.setReceiverRateSnapshot(receiverRateUsd);

        BigDecimal receiverBalanceEffect = usdAmount.multiply(receiverRateUsd);

        transaction.setReceiverBalanceEffect(receiverBalanceEffect.negate());
    }
}