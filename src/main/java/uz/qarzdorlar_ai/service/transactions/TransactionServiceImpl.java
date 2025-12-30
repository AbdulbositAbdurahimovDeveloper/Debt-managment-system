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
import uz.qarzdorlar_ai.exception.BadCredentialsException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.ExchangeRateMapper;
import uz.qarzdorlar_ai.mapper.TransactionMapper;
import uz.qarzdorlar_ai.model.*;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.TransactionItemCreateDTO;
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
    private final ExchangeRateMapper exchangeRateMapper;
    private final ExchangeRateRepository exchangeRateRepository;


    @Override
    public TransactionDTO createTransaction(TransactionCreateDTO dto, User staffUser) {

        TransactionType type = dto.getType();

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found with id : " + dto.getClientId())
                );

        Currency currency = currencyRepository.findById(dto.getCurrencyId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id : " + dto.getCurrencyId())
                );


        User user = userRepository.findByUsername(staffUser.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id : " + staffUser.getId())
                );

        Role role = user.getRole();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCurrency(currency);
        transaction.setDescription(dto.getDescription());
        if (dto.getCreatedAt() != null) {
            transaction.setCreatedAt(dto.getCreatedAt());
        }

        switch (type) {
            case SALE -> handleTransactionSale(dto, transaction, currency);
//            case PAYMENT -> handleTransactionPayment();
//            case RETURN -> handleTransactionReturn();
//            case RETURN_PAYMENT -> handleTransactionReturnPayment();
//            case PURCHASE -> handleTransactionReturnPurchase();
//            case TRANSFER -> handleTransactionReturnTransfer();
        }


        return null;
    }

    private void handleTransactionSale(TransactionCreateDTO dto, Transaction transaction, Currency currency) {


        List<TransactionItemCreateDTO> items = dto.getItems();
        if (items == null) {
            throw new BadCredentialsException("Product sale be not null");
        }

        List<TransactionItem> transactionItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

//        for (TransactionItemCreateDTO item : items) {
//            Long productId = item.getProductId();
//            Integer quantity = item.getQuantity();
//
//            Product product = productRepository.findById(productId)
//                    .orElseThrow(() ->
//                            new EntityNotFoundException("Product not found with id : " + productId)
//                    );
//
//            BigDecimal priceUsd = product.getPriceUsd();
//            BigDecimal unitPrice = item.getUnitPrice() != null
//                    ? item.getUnitPrice()
//                    : priceUsd;
//
//            BigDecimal calculatePrice;
//
//            if (currency.getCode().equals("USD")) {
//
//                calculatePrice = unitPrice.multiply(exchangeRate);
//
//            } else {
//
//                calculatePrice =
//
//
//            }
//
//            TransactionItem transactionItem = new TransactionItem();
//            transactionItem.setProduct(product);
//            transactionItem.setQuantity(quantity);
//            transactionItem.setUnitPrice(item.getUnitPrice());
//            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
//            transactionItem.setTotalPrice(totalPrice);
//
//            transactionItems.add(transactionItem);
//            totalAmount.add(totalPrice);
//
//        }
        for (TransactionItemCreateDTO item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id : " + productId));

            // Agar DTO dan narx kelsa u Transaction valyutasida deb hisoblaymiz,
            // bo'lmasa mahsulotning bazadagi USD narxini olamiz
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : product.getPriceUsd();

            BigDecimal itemUsdAmount;

            if (currency.getCode().equals("USD")) {
                // Tranzaksiya USD da bo'lsa, USD narxni to'g'ridan-to'g'ri ko'paytiramiz
                itemUsdAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            } else {
                // Tranzaksiya boshqa valyutada (so'm, dirham) bo'lsa, uni USD ga o'giramiz: (Narx / Kurs) * Soni
                itemUsdAmount = unitPrice.divide(exchangeRate, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(quantity));
            }

            // Ushbu mahsulotning mijoz valyutasidagi (balanceCurrency) qiymati
            BigDecimal itemBalancePrice = itemUsdAmount.multiply(clientExchangeRate);

            // Umumiy summani mijoz valyutasida yig'ib boramiz
            totalAmount = totalAmount.add(itemBalancePrice);

            // TransactionItem obyektini tayyorlash (bazada USD da saqlash tavsiya etiladi)
            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setProduct(product);
            transactionItem.setQuantity(quantity);

            transactionItem.setUnitPrice(
                    itemUsdAmount.divide(
                            BigDecimal.valueOf(quantity),
                            6,
                            RoundingMode.HALF_UP)
            ); // 1 dona USD dagi narxi

            transactionItem.setTotalPrice(itemUsdAmount); // Jami USD dagi narxi
            transactionItems.add(transactionItem);
        }

        transaction.setUsdAmount(totalAmount);
        transaction.setBalanceAmount(transaction.getBalanceAmount().add(totalAmount));


    }

    public static BigDecimal safeRate(BigDecimal rate) {
        return (rate == null || rate.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ONE
                : rate;
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
