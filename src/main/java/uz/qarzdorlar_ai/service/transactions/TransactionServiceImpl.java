package uz.qarzdorlar_ai.service.transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.TransactionMapper;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.model.TransactionItem;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.*;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.repository.ProductRepository;
import uz.qarzdorlar_ai.repository.TransactionRepository;
import uz.qarzdorlar_ai.repository.UserRepository;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionCalculationService;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionHelperService;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionService;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionCalculationService transactionCalculationService;
    private final TransactionHelperService transactionHelperService;

    private static final EnumSet<Role> ALLOWED_ROLES = EnumSet.of(Role.ADMIN, Role.DEVELOPER, Role.STAFF, Role.STAFF_PLUS);


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
        transaction.setCreatedBy(user);
        transaction.setType(type);
        transaction.setTransactionCurrency(dto.getTransactionCurrency());
        transaction.setDescription(dto.getDescription());

        // createdAt - agar DTO dan kelmasa, current date ishlatiladi (AbsDateEntity @PrePersist tufayli)
        // Agar DTO dan kelgan bo'lsa, o'sha sanani ishlatamiz (migratsiya uchun)
        if (dto.getCreatedAt() != null) {
            transaction.setCreatedAt(dto.getCreatedAt());
        }

        // Har bir transaction type uchun alohida logika
        transactionCalculationService.calculateTransaction(dto, transaction, client);

        // Transactionni saqlash
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Client balance yangilash
        transactionHelperService.updateClientBalance(savedTransaction);

        return transactionMapper.toDTO(savedTransaction);
    }

    @Override
    @Transactional
    public TransactionDTO updateTransaction(Long id, TransactionUpdateDTO dto, User staffUser) {
        // 1. Eski tranzaksiyani topish
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction topilmadi. ID: " + id));

        // Xavfsizlik tekshiruvi
        if (!ALLOWED_ROLES.contains(staffUser.getRole())) {
            throw new AccessDeniedException("Sizda tahrirlash huquqi yo'q!");
        }

        // 2. MUHIM: Eski balansni orqaga qaytarish (Hali ma'lumotlar o'zgarmasdan turib)
        transactionHelperService.reverseClientBalance(tx);

        // 3. QISMAN YANGILASH (Null-safe Merge)
        // Faqat DTO da null bo'lmagan maydonlarni yangilaymiz
        if (dto.getAmount() != null) tx.setAmount(dto.getAmount());
        if (dto.getMarketRate() != null) tx.setMarketRate(dto.getMarketRate());
        if (dto.getClientRate() != null) tx.setClientRate(dto.getClientRate());
        if (dto.getReceiverRate() != null) tx.setReceiverRate(dto.getReceiverRate());
        if (dto.getFeeAmount() != null) tx.setFeeAmount(dto.getFeeAmount());
        if (dto.getTransactionCurrency() != null) tx.setTransactionCurrency(dto.getTransactionCurrency());
        if (dto.getDescription() != null) tx.setDescription(dto.getDescription());

        // 4. Mahsulotlarni (Items) yangilash logikasi
        // Agar DTO da items kelsa, ularni butunlay yangilaymiz
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            tx.getItems().clear(); // OrphanRemoval=true eski itemlarni o'chiradi
        }

        // 5. QAYTA HISOBLASH (Merged ma'lumotlar asosida)
        // Muhim: calculationDto ni tx (entity) dagi yangilangan ma'lumotlardan yig'amiz
        TransactionCreateDTO calculationDto = mapToCalculationDto(tx, dto.getItems());

        transactionCalculationService.calculateTransaction(calculationDto, tx, tx.getClient());

        // 6. Yangi hisoblangan balansni qo'llash
        transactionHelperService.updateClientBalance(tx);

        // 7. Saqlash
        Transaction updatedTx = transactionRepository.save(tx);

        return transactionMapper.toDTO(updatedTx);
    }

    private TransactionCreateDTO mapToCalculationDto(Transaction tx, List<TransactionItemCreateDTO> newItems) {
        TransactionCreateDTO createDto = new TransactionCreateDTO();
        createDto.setType(tx.getType());

        // Entity dagi (merged qilingan) ma'lumotlarni olamiz
        createDto.setAmount(tx.getAmount());
        createDto.setMarketRate(tx.getMarketRate());
        createDto.setClientRate(tx.getClientRate());
        createDto.setReceiverRate(tx.getReceiverRate());
        createDto.setFeeAmount(tx.getFeeAmount());
        createDto.setTransactionCurrency(tx.getTransactionCurrency());

        // Agar yangi itemlar kelsa ularni, kelmasa mavjudlarini beramiz
        if (newItems != null && !newItems.isEmpty()) {
            createDto.setItems(newItems);
        } else {
            // Mavjud itemlarni DTO formatiga o'tkazish (agar kerak bo'lsa)
            createDto.setItems(mapItemsToCreateDto(tx.getItems()));
        }

        return createDto;
    }

    private List<TransactionItemCreateDTO> mapItemsToCreateDto(List<TransactionItem> items) {
        if (items == null) return List.of();
        return items.stream().map(item -> {
            TransactionItemCreateDTO itemDto = new TransactionItemCreateDTO();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice()); // TC dagi narxi
            return itemDto;
        }).collect(Collectors.toList());
    }

//    @Override
//    @Transactional
//    public TransactionDTO updateTransaction(Long id, TransactionUpdateDTO dto, User staffUser) {
//        // Transaction ni topish
//        Transaction transaction = transactionRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));
//
//        // User huquqini tekshirish
//        User user = userRepository.findByUsername(staffUser.getUsername())
//                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + staffUser.getId()));
//
//        Role role = user.getRole();
//        if (!ALLOWED_ROLES.contains(role)) {
//            throw new AccessDeniedException("You are not allowed to perform this action");
//        }
//
//        // Eski balansni qaytarish (revert) - avval eski transaction ning ta'sirini bekor qilamiz
////        transactionHelperService.revertClientBalance(transaction);
//
//        // Yangi ma'lumotlarni o'rnatish
//        boolean needsRecalculation = false;
//
//        // Client o'zgartirilgan bo'lsa
//        if (dto.getClientId() != null && !dto.getClientId().equals(transaction.getClient().getId())) {
//            Client newClient = clientRepository.findById(dto.getClientId())
//                    .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + dto.getClientId()));
//            transaction.setClient(newClient);
//            needsRecalculation = true;
//        }
//
//        // Receiver client o'zgartirilgan bo'lsa (TRANSFER uchun)
//        if (dto.getReceiverClientId() != null) {
//            if (transaction.getType() != TransactionType.TRANSFER) {
//                throw new BadRequestException("Receiver client can only be set for TRANSFER transactions");
//            }
//            Client newReceiverClient = clientRepository.findById(dto.getReceiverClientId())
//                    .orElseThrow(() -> new EntityNotFoundException("Receiver client not found with id: " + dto.getReceiverClientId()));
//            transaction.setReceiverClient(newReceiverClient);
//            needsRecalculation = true;
//        }
//
//        // Currency o'zgartirilgan bo'lsa
////        if (dto.getCurrencyId() != null && !dto.getCurrencyId().equals(transaction.getCurrency().getId())) {
////            Currency newCurrency = currencyRepository.findById(dto.getCurrencyId())
////                    .orElseThrow(() -> new EntityNotFoundException("Currency not found with id: " + dto.getCurrencyId()));
////            transaction.setCurrency(newCurrency);
////            needsRecalculation = true;
////        }
//
//        // Exchange rate o'zgartirilgan bo'lsa
//        if (dto.getExchangeRate() != null) {
//            transaction.setExchangeRate(dto.getExchangeRate());
//            needsRecalculation = true;
//        }
//
//        // Client exchange rate o'zgartirilgan bo'lsa
//        if (dto.getClientExchangeRate() != null) {
//            transaction.setClientExchangeRate(dto.getClientExchangeRate());
//            needsRecalculation = true;
//        }
//
//        // Receiver exchange rate o'zgartirilgan bo'lsa (TRANSFER uchun)
//        if (dto.getReceiverExchangeRate() != null) {
//            if (transaction.getType() != TransactionType.TRANSFER) {
//                throw new BadRequestException("Receiver exchange rate can only be set for TRANSFER transactions");
//            }
//            transaction.setReceiverExchangeRate(dto.getReceiverExchangeRate());
//            needsRecalculation = true;
//        }
//
//        // Original amount o'zgartirilgan bo'lsa
//        if (dto.getOriginalAmount() != null) {
//            transaction.setOriginalAmount(dto.getOriginalAmount());
//            needsRecalculation = true;
//        }
//
//        // Fee amount o'zgartirilgan bo'lsa
//        if (dto.getFeeAmount() != null) {
//            transaction.setFeeAmount(dto.getFeeAmount());
//            needsRecalculation = true;
//        }
//
//        // Description o'zgartirilgan bo'lsa
//        if (dto.getDescription() != null) {
//            transaction.setDescription(dto.getDescription());
//        }
//
//        // Items o'zgartirilgan bo'lsa (SALE, RETURN, PURCHASE uchun)
//        if (dto.getItems() != null) {
//            TransactionType type = transaction.getType();
//            if (type != TransactionType.SALE && type != TransactionType.RETURN && type != TransactionType.PURCHASE) {
//                throw new BadRequestException("Items can only be updated for SALE, RETURN, or PURCHASE transactions");
//            }
//            // Eski items larni o'chirish
//            if (transaction.getItems() != null) {
//                transaction.getItems().clear();
//            }
//            needsRecalculation = true;
//        }
//
//        // Status o'zgartirilgan bo'lsa
//        if (dto.getStatus() != null) {
//            transaction.setStatus(dto.getStatus());
//        }
//
//        // Agar qayta hisoblash kerak bo'lsa, qayta hisoblaymiz
//        if (needsRecalculation) {
////            transactionCalculationService.recalculateTransaction(transaction, dto);
//        }
//
//        // Transactionni saqlash
//        Transaction savedTransaction = transactionRepository.save(transaction);
//
//        // Yangi balansni qo'llash

    /// /        transactionHelperService.updateClientBalance(savedTransaction);
//
//        return transactionMapper.toDTO(null);
//    }
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
        transactionHelperService.reverseClientBalance(transaction);

        // Transaction ni soft delete qilish
        // @SQLDelete annotation tufayli deleted = true bo'ladi
        transactionRepository.delete(transaction);
        log.info("Transaction {} deleted by user {}", id, staffUser.getUsername());
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
