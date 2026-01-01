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
            case PAYMENT -> calculatePaymentTransaction(dto, transaction, client);
            case RETURN -> calculateReturnTransaction(dto, transaction, client);
            case RETURN_PAYMENT -> calculateReturnPaymentTransaction(dto, transaction, client);
            case PURCHASE -> calculatePurchaseTransaction(dto, transaction, client);
            case PURCHASE_PAYMENT -> calculatePurchasePaymentTransaction(dto, transaction, client);
            case TRANSFER -> calculateTransferTransaction(dto, transaction, client);
            default -> throw new BadRequestException("Unsupported transaction type: " + transaction.getType());
        }


    }

    private void calculateTransferTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: Mahsulotlar bo'lmasligi kerak
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            throw new BadRequestException("TRANSFER tranzaksiyasi uchun mahsulotlar kiritilishi mumkin emas!");
        }

        // 2. Qabul qiluvchi (Receiver) mavjudligini tekshirish
        if (dto.getReceiverClientId() == null) {
            throw new BadRequestException("Pulni qabul qiluvchi (Receiver) ko'rsatilishi shart!");
        }

        // ReceiverClient entity'sini DB dan olish (ID orqali)
        Client receiver = clientRepository.findById(dto.getReceiverClientId())
                .orElseThrow(() -> new EntityNotFoundException("Qabul qiluvchi mijoz topilmadi!"));

        if (client.getId().equals(receiver.getId())) {
            throw new BadRequestException("O'z-o'ziga pul o'tkazish mumkin emas!");
        }

        // 3. Kurslarni aniqlash (USD Pivot)
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        // Beruvchi (Sender) kurs: USD -> SenderCurrency
        BigDecimal sRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        // Oluvchi (Receiver) kurs: USD -> ReceiverCurrency
        BigDecimal rRate = dto.getReceiverRate() != null ? dto.getReceiverRate() : BigDecimal.ONE;

        // 4. Summani hisoblash
        BigDecimal transferAmount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("O'tkazma summasi noldan katta bo'lishi kerak!");
        }

        // 5. Entity maydonlarini to'ldirish
        transaction.setReceiverClient(receiver);
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setAmount(transferAmount); // Naqd o'tkazilgan summa (TCda)
        transaction.setMarketRate(mRate);

        // USD Pivot (O'tkazmaning dollardagi qiymati)
        BigDecimal usdAmount = transferAmount.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);

        // Xizmat haqi (Fee) - Masalan, o'tkazma uchun olingan komissiya
        if (dto.getFeeAmount() != null && dto.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            transaction.setFeeAmount(dto.getFeeAmount().divide(mRate, 6, RoundingMode.HALF_UP));
        } else {
            transaction.setFeeAmount(BigDecimal.ZERO);
        }

        // 6. IKKALA MIJOZ BALANSIGA TA'SIRNI HISOBLASH

        // Beruvchi (Sender) uchun snapshot
        transaction.setClientCurrency(client.getCurrencyCode());
        transaction.setClientRate(sRate);
        // Beruvchi balansidan usdAmount * sRate miqdorida pul chiqadi
        transaction.setBalanceEffect(usdAmount.multiply(sRate));

        // Oluvchi (Receiver) uchun snapshot
        transaction.setReceiverRate(rRate);
        // Oluvchi balansiga usdAmount * rRate miqdorida pul kiradi
        // (Buni updateBalance service'da receiverRate orqali hisoblab olasiz)

        transaction.setStatus(TransactionStatus.COMLATED);
    }

    private void calculatePurchasePaymentTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: To'lovda mahsulotlar bo'lishi mumkin emas
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            throw new BadRequestException("Purchase payment tranzaksiyasi uchun mahsulotlar kiritilishi mumkin emas!");
        }

        // 2. Summani tekshirish
        BigDecimal cashAmount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;
        if (cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("To'lov summasi noldan katta bo'lishi kerak!");
        }

        // 3. Kurslarni aniqlash (USD Pivot qoidasi)
        // MarketRate: To'langan valyutani USDga keltirish uchun
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        // ClientRate: Taminotchining balans valyutasi kursi (USD -> SupplierCurrency)
        BigDecimal cRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        // 4. Entity maydonlarini to'ldirish
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setAmount(cashAmount); // Biz bergan naqd pul (Masalan: 10,000,000 UZS)
        transaction.setMarketRate(mRate);

        // USD Pivot qiymati (10,000,000 / 12,800 = 781.25 USD)
        BigDecimal usdAmount = cashAmount.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);

        // 5. Xizmat haqi (Fee)
        // Agar taminotchiga pulni kuryer orqali yuborgan bo'lsak va kuryer xizmat haqi olsa
        if (dto.getFeeAmount() != null && dto.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            transaction.setFeeAmount(dto.getFeeAmount().divide(mRate, 6, RoundingMode.HALF_UP));
        } else {
            transaction.setFeeAmount(BigDecimal.ZERO);
        }

        // 6. TAMINOTCHI BALANSIGA TA'SIR
        transaction.setClientCurrency(client.getCurrencyCode());
        transaction.setClientRate(cRate);

        // balanceEffect: Taminotchining bizdagi haqi (bizning qarzimiz) kamayishi
        // usdAmount * clientRate
        BigDecimal effect = usdAmount.multiply(cRate);
        transaction.setBalanceEffect(effect);

        transaction.setStatus(TransactionStatus.COMLATED);
    }

    private void calculatePurchaseTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: Sotib olishda mahsulotlar bo'lishi shart
        List<TransactionItemCreateDTO> itemDTOs = dto.getItems();
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            throw new BadRequestException("Purchase tranzaksiyasi uchun mahsulotlar kiritilishi shart!");
        }

        // 2. Kurslarni aniqlash (USD Pivot)
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        // Taminotchining balansi qaysi valyutada yuritilsa, o'sha kurs (clientRate)
        BigDecimal cRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalPurchaseAmountInTC = BigDecimal.ZERO; // Tranzaksiya valyutasida (UZS/USD...)

        // 3. Mahsulotlarni hisoblash
        for (TransactionItemCreateDTO itemDto : itemDTOs) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Mahsulot topilmadi: " + itemDto.getProductId()));

            // Taminotchi bizga qaysi narxda sotayotgani (unitPrice)
            // Agar DTOda berilmagan bo'lsa, bazadagi USD narxni kursga urib olamiz
            BigDecimal unitPriceInTC = (itemDto.getUnitPrice() != null)
                    ? itemDto.getUnitPrice()
                    : product.getPriceUsd().multiply(mRate);

            BigDecimal itemTotalInTC = unitPriceInTC.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalPurchaseAmountInTC = totalPurchaseAmountInTC.add(itemTotalInTC);

            // TransactionItem yaratish
            TransactionItem item = new TransactionItem();
            item.setTransaction(transaction);
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            // Narxlar bazada USDda muhrlanadi (unitPriceInTC / mRate)
            item.setUnitPrice(unitPriceInTC.divide(mRate, 6, RoundingMode.HALF_UP));
            item.setTotalPrice(itemTotalInTC.divide(mRate, 6, RoundingMode.HALF_UP));
            transactionItems.add(item);
        }

        // 4. Qo'shimcha xarajatlar (Masalan: Yuk tashish haqi - Fee)
        // Agar taminotchi yukni yetkazib berishni ham narxga qo'shsa
        BigDecimal feeInTC = dto.getFeeAmount() != null ? dto.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal totalWithFeeInTC = totalPurchaseAmountInTC.add(feeInTC);

        // 5. Entity maydonlarini to'ldirish
        transaction.setItems(transactionItems);
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setMarketRate(mRate);

        // amount: Jami taminotchidan olingan yuk qiymati (TCda)
        transaction.setAmount(totalWithFeeInTC);

        // usdAmount: USD Pivot qiymati
        BigDecimal totalUsd = totalWithFeeInTC.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(totalUsd);

        // feeAmount: USD da saqlangan xizmat haqi
        transaction.setFeeAmount(feeInTC.divide(mRate, 6, RoundingMode.HALF_UP));

        // 6. MIJOZ (TAMINOTCHI) BALANSIGA TA'SIR
        transaction.setClientCurrency(client.getCurrencyCode());
        transaction.setClientRate(cRate);

        // balanceEffect: Taminotchining bizdagi haqi (qarzimiz) ortishi
        // usdAmount * clientRate
        transaction.setBalanceEffect(totalUsd.multiply(cRate));

        transaction.setStatus(TransactionStatus.COMLATED);
    }

    private void calculateReturnPaymentTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: Pul qaytarishda mahsulotlar bo'lmaydi
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            throw new BadRequestException("Return payment tranzaksiyasi uchun mahsulotlar kiritilishi mumkin emas!");
        }

        // 2. Summani tekshirish
        BigDecimal cashAmount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;
        if (cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Qaytariladigan summa noldan katta bo'lishi kerak!");
        }

        // 3. Kurslarni aniqlash (USD Pivot)
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        BigDecimal cRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        // 4. Entity maydonlarini to'ldirish
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setAmount(cashAmount); // Mijozga berilgan naqd pul (Masalan: 1,280,000 UZS)
        transaction.setMarketRate(mRate);

        // USD Pivot qiymati (1,280,000 / 12,800 = 100 USD)
        BigDecimal usdAmount = cashAmount.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);

        // 5. Xizmat haqi (Fee)
        // Agar kuryer pulni mijozga olib borib bergani uchun haq olsa
        if (dto.getFeeAmount() != null && dto.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            transaction.setFeeAmount(dto.getFeeAmount().divide(mRate, 6, RoundingMode.HALF_UP));
        } else {
            transaction.setFeeAmount(BigDecimal.ZERO);
        }

        // 6. MIJOZ BALANSIGA TA'SIR
        transaction.setClientCurrency(client.getCurrencyCode());
        transaction.setClientRate(cRate);

        // BalanceEffect: Mijoz valyutasidagi summa
        // RETURN_PAYMENT mijozning qarzini oshiradi (biz unga pul berdik)
        // 100 USD * 3.67 = 367 AED
        BigDecimal effect = usdAmount.multiply(cRate);
        transaction.setBalanceEffect(effect);

        transaction.setStatus(TransactionStatus.COMLATED);
    }

    private void calculateReturnTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: Qaysi mahsulotlar qaytayotganini bilishimiz shart
        List<TransactionItemCreateDTO> itemDTOs = dto.getItems();
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            throw new BadRequestException("Return tranzaksiyasi uchun mahsulotlar kiritilishi shart!");
        }

        // 2. Kurslarni aniqlash
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        BigDecimal cRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalReturnAmountInTC = BigDecimal.ZERO; // Tranzaksiya valyutasida (UZS/AED...)

        // 3. Qaytarilayotgan mahsulotlarni hisoblash
        for (TransactionItemCreateDTO itemDto : itemDTOs) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Mahsulot topilmadi: " + itemDto.getProductId()));

            // Qaytarish narxi: Odatda sotilgan narxda qaytariladi.
            // Agar DTOda maxsus narx kelmasa, bazadagi narxni kursga urib olamiz.
            BigDecimal unitPriceInTC = (itemDto.getUnitPrice() != null)
                    ? itemDto.getUnitPrice()
                    : product.getPriceUsd().multiply(mRate);

            BigDecimal itemTotalInTC = unitPriceInTC.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalReturnAmountInTC = totalReturnAmountInTC.add(itemTotalInTC);

            // TransactionItem (Snapshot)
            TransactionItem item = new TransactionItem();
            item.setTransaction(transaction);
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            // USD narxini saqlaymiz
            item.setUnitPrice(unitPriceInTC.divide(mRate, 6, RoundingMode.HALF_UP));
            item.setTotalPrice(itemTotalInTC.divide(mRate, 6, RoundingMode.HALF_UP));
            transactionItems.add(item);
        }

        // 4. Vozvratda xizmat haqi (Masalan: Restocking fee yoki kuryer xizmati)
        // Agar mijoz vozvrat qilgani uchun kuryerga pul to'lashimiz kerak bo'lsa:
        BigDecimal feeInTC = dto.getFeeAmount() != null ? dto.getFeeAmount() : BigDecimal.ZERO;

        // 5. Entity maydonlarini to'ldirish
        transaction.setItems(transactionItems);
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setMarketRate(mRate);

        // Vozvrat summasi (Mahsulotlar qiymati)
        transaction.setAmount(totalReturnAmountInTC);

        // USD Pivot
        BigDecimal usdAmount = totalReturnAmountInTC.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);

        // Kuryer/Xizmat haqi
        transaction.setFeeAmount(feeInTC.divide(mRate, 6, RoundingMode.HALF_UP));

        // 6. MIJOZ BALANSIGA TA'SIR
        transaction.setClientCurrency(client.getCurrencyCode());
        transaction.setClientRate(cRate);

        // BalanceEffect: Vozvrat summasidan xizmat haqini ayirib yuboramiz (agar fee bo'lsa)
        // Chunki fee — mijozning foydasini (vozvrat summasini) kamaytiradi.
        BigDecimal finalUsd = usdAmount.subtract(transaction.getFeeAmount());
        transaction.setBalanceEffect(finalUsd.multiply(cRate));

        transaction.setStatus(TransactionStatus.COMLATED);
    }

    private void calculatePaymentTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: To'lovda mahsulotlar bo'lishi mumkin emas
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            throw new BadRequestException("To'lov tranzaksiyasi uchun mahsulotlar kiritilishi mumkin emas!");
        }

        // 2. Kurslarni aniqlash
        // MarketRate: Tranzaksiya valyutasini USDga o'girish uchun (Masalan: UZS -> USD)
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        // ClientRate: USDni mijozning balans valyutasiga o'girish uchun (Masalan: USD -> AED)
        BigDecimal cRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        // 3. Summalarni hisoblash
        BigDecimal cashAmount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;

        // 4. Entity maydonlarini to'ldirish
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setAmount(cashAmount); // Naqd olingan summa (UZSda)
        transaction.setMarketRate(mRate);

        // USD Pivot (Hamma narsa bazada USD qiymatida muhrlanishi shart)
        BigDecimal usdAmount = cashAmount.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);

        // 5. Xizmat haqi (Fee) - Agar kuryer pulni olib kelgan bo'lsa
        if (dto.getFeeAmount() != null && dto.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Fee ham USDga o'girilib saqlanadi
            transaction.setFeeAmount(dto.getFeeAmount().divide(mRate, 6, RoundingMode.HALF_UP));
        } else {
            transaction.setFeeAmount(BigDecimal.ZERO);
        }

        // 6. MIJOZ BALANSIGA TA'SIR (Kredit/To'lov)
        transaction.setClientCurrency(client.getCurrencyCode());
        transaction.setClientRate(cRate);

        // balanceEffect: Mijozning o'z valyutasida qarzidan chegiriladigan summa
        // Formula: usdAmount * clientRate
        BigDecimal effect = usdAmount.multiply(cRate);
        transaction.setBalanceEffect(effect);

        // Status va boshqalar
        transaction.setStatus(TransactionStatus.COMLATED);
    }

    private void calculateSaleTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {
        // 1. Validatsiya: Sotuvda mahsulotlar bo'lishi shart
        List<TransactionItemCreateDTO> itemDTOs = dto.getItems();
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            throw new BadRequestException("Sotuv tranzaksiyasi uchun mahsulotlar kiritilishi shart!");
        }

        // 2. Kurslarni aniqlash (USD bo'lsa 1, aks holda MarketRate)
        BigDecimal mRate = CurrencyCode.USD.equals(dto.getTransactionCurrency())
                ? BigDecimal.ONE
                : dto.getMarketRate();

        // Mijozning shaxsiy kursi (Mijoz balans valyutasiga o'girish uchun)
        BigDecimal cRate = dto.getClientRate() != null ? dto.getClientRate() : BigDecimal.ONE;

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalTransactionAmount = BigDecimal.ZERO; // Tranzaksiya valyutasida (UZS/AED...)

        // 3. Mahsulotlarni aylanish va USD qiymatga keltirish
        for (TransactionItemCreateDTO itemDto : itemDTOs) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Mahsulot topilmadi: " + itemDto.getProductId()));

            // Narxni aniqlash: Agar DTOda bo'lmasa, bazadagi USD narxni kursga ko'paytiramiz
            BigDecimal unitPriceInTC = (itemDto.getUnitPrice() != null)
                    ? itemDto.getUnitPrice()
                    : product.getPriceUsd().multiply(mRate);

            BigDecimal itemTotalInTC = unitPriceInTC.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalTransactionAmount = totalTransactionAmount.add(itemTotalInTC);

            // TransactionItem yaratish (Narxlar har doim USDda saqlanadi - pivot qoida)
            TransactionItem item = new TransactionItem();
            item.setTransaction(transaction);
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            // USD qiymat = Tranzaksiya summasi / MarketRate
            item.setUnitPrice(unitPriceInTC.divide(mRate, 6, RoundingMode.HALF_UP));
            item.setTotalPrice(itemTotalInTC.divide(mRate, 6, RoundingMode.HALF_UP));
            transactionItems.add(item);
        }

        // 4. Xizmat haqi (Fee) mantiqi (Tranzaksiya valyutasidan USDga o'girish)
        BigDecimal feeInTC = dto.getFeeAmount() != null ? dto.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal totalWithFeeInTC = totalTransactionAmount.add(feeInTC);

        // 5. Entity maydonlarini to'ldirish (Siz bergan Entityga mos)
        transaction.setItems(transactionItems);
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setMarketRate(mRate);

        // amount: Amalda berilgan jami summa (Mahsulotlar + Xizmat haqi)
        transaction.setAmount(totalWithFeeInTC);

        // usdAmount: Tizim ichki USD qiymati (Pivot)
        BigDecimal totalUsd = totalWithFeeInTC.divide(mRate, 6, RoundingMode.HALF_UP);
        transaction.setUsdAmount(totalUsd);

        // feeAmount: Kuryer haqi (Siz aytgandek USD da saqlaymiz)
        transaction.setFeeAmount(feeInTC.divide(mRate, 6, RoundingMode.HALF_UP));

        // 6. Mijoz Balansiga ta'sir
        transaction.setClientCurrency(client.getCurrencyCode()); // Snapshot
        transaction.setClientRate(cRate);

        // balanceEffect: Mijoz valyutasidagi yakuniy summa (usdAmount * clientRate)
        // Masalan: $1000 * 3.67 = 3670 AED
        transaction.setBalanceEffect(totalUsd.multiply(cRate));

        // Statusni o'rnatish
        transaction.setStatus(TransactionStatus.COMLATED); // Siz yozgan typo bo'yicha
    }

    /*
    private void calculateSaleTransaction(TransactionCreateDTO dto, Transaction transaction, Client client) {

        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
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

            BigDecimal basePriceInTransactionCurrency = product.getPriceUsd().multiply(dto.getMarketRate());
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
            if (CurrencyCode.USD.equals(dto.getTransactionCurrency())) {
                itemUsdAmount = unitPriceInTransactionCurrency.multiply(BigDecimal.valueOf(quantity));
            } else {
                itemUsdAmount = unitPriceInTransactionCurrency
                        .divide(dto.getMarketRate(), 6, RoundingMode.HALF_UP)
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
            if (CurrencyCode.USD.equals(dto.getTransactionCurrency())) {
                feeAmountInUsd = feeAmount;
            } else {
                feeAmountInUsd = feeAmount.divide(dto.getMarketRate(), 6, RoundingMode.HALF_UP);
            }
        }

        transaction.setMarketRate(dto.getMarketRate());
        transaction.setClientRate(dto.getClientRate());
        transaction.setUsdAmount(totalUsdAmount);
        transaction.setClientCurrency(client.getCurrencyCode());

        // MUHIM: SALE holatida fee qarzga QO'SHILADI (qarz ko'payadi), ayirilmaydi
        BigDecimal totalBalanceAmount = totalUsdAmount.multiply(dto.getMarketRate());
        BigDecimal feeBalanceAmount = feeAmountInUsd.multiply(dto.getMarketRate());
        transaction.setBalanceEffect(totalBalanceAmount.add(feeBalanceAmount));

        transaction.setItems(transactionItems);

    }
    */
}