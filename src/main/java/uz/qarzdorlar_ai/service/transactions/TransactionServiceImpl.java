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
import uz.qarzdorlar_ai.repository.TransactionRepository;
import uz.qarzdorlar_ai.repository.UserRepository;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionCalculationService;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionHelperService;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final ClientRepository clientRepository;
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
        // 1. Eskisini topish
        Transaction tx = transactionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Tranzaksiya topilmadi ID: " + id));

        // 2. REVERSE: Bazadagi hozirgi ta'sirni bekor qilish (Mijoz balansini joyiga qaytarish)
        transactionHelperService.reverseClientBalance(tx);

        // 3. CLEAR ITEMS: Eski mahsulotlarni o'chirish (orphanRemoval ishlaydi)
        tx.getItems().clear();
        transactionRepository.saveAndFlush(tx); // Bazani majburan tozalash

        // 4. MERGE: Hisoblash uchun to'liq ma'lumotni shakllantirish
        TransactionCreateDTO calculationDto = mergeToCalculationDto(tx, dto);

        // 5. RECALCULATE: Mavjud Entity (tx) ni yangi DTO asosida qayta hisoblash
        // calculateTransaction ichida tx.setAmount, tx.setBalanceEffect va h.k.lar yangilanadi
        transactionCalculationService.calculateTransaction(calculationDto, tx, tx.getClient());

        // 6. QO'SHIMCHA MA'LUMOTLAR
        if (dto.getDescription() != null) tx.setDescription(dto.getDescription());
        // Tranzaksiya statusini update qilish mumkin bo'lsa:
        // tx.setStatus(TransactionStatus.COMPLETED);

        // 7. APPLY NEW BALANCE: Yangi hisoblangan raqamlarni mijoz balansiga qo'shish
        transactionHelperService.updateClientBalance(tx);

        // 8. SAVE: Hammasini bitta atomar tranzaksiyada saqlash
        Transaction updatedTx = transactionRepository.save(tx);

        return transactionMapper.toDTO(updatedTx);
    }

    private TransactionCreateDTO mergeToCalculationDto(Transaction tx, TransactionUpdateDTO updateDto) {
        TransactionCreateDTO calcDto = new TransactionCreateDTO();

        // 1. O'zgarmas maydonlar (Type va Client o'zgarmasligi tavsiya etiladi)
        calcDto.setType(tx.getType());
        calcDto.setClientId(tx.getClient().getId());
        calcDto.setReceiverClientId(tx.getReceiverClient() != null ? tx.getReceiverClient().getId() : updateDto.getReceiverClientId());

        // 2. Valyuta va Kurslar (DTOda bo'lsa yangisi, bo'lmasa eskidagisi)
        calcDto.setTransactionCurrency(updateDto.getTransactionCurrency() != null ?
                updateDto.getTransactionCurrency() : tx.getTransactionCurrency());

        calcDto.setRateToUsd(updateDto.getRateToUsd() != null ?
                updateDto.getRateToUsd() : tx.getRateToUsd());

        calcDto.setClientRateToUsd(updateDto.getClientRateToUsd() != null ?
                updateDto.getClientRateToUsd() : tx.getClientRateSnapshot());

        calcDto.setReceiverRateToUsd(updateDto.getReceiverRateToUsd() != null ?
                updateDto.getReceiverRateToUsd() : tx.getReceiverRateSnapshot());

        // 3. Summa va Fee
        calcDto.setAmount(updateDto.getAmount() != null ? updateDto.getAmount() : tx.getAmount());

        // FeeAmount bazada doim USDda turadi. DTOda ham USDda kelyapti deb hisoblaymiz.
        calcDto.setFeeAmount(updateDto.getFeeAmount() != null ? updateDto.getFeeAmount() : tx.getFeeAmount());

        // 4. MAHSULOTLAR (Items) - Eng muhim joyi
        if (updateDto.getItems() != null && !updateDto.getItems().isEmpty()) {
            // Agar Frontend yangi list yuborgan bo'lsa, o'shani olamiz
            calcDto.setItems(updateDto.getItems());
        } else {
            // Agar Frontend items yubormagan bo'lsa, eskisini DTO formatiga o'girib beramiz
            calcDto.setItems(mapExistingItemsToDto(tx.getItems()));
        }

        return calcDto;
    }

    // Eskilarini DTO formatiga o'tkazish yordamchi metodi
    private List<TransactionItemCreateDTO> mapExistingItemsToDto(List<TransactionItem> items) {
        if (items == null) return new ArrayList<>();
        return items.stream().map(item -> {
            TransactionItemCreateDTO itemDto = new TransactionItemCreateDTO();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice()); // Tranzaksiya valyutasidagi narxi
            return itemDto;
        }).collect(Collectors.toList());
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

    @Override
    public PageDTO<TransactionDTO> getAllTransactionByClientId(Long clientId, Integer page, Integer size) {

        Sort sort = Sort.by(AbsDateEntity.Fields.createdAt);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Transaction> transactions = transactionRepository.findAllByClientOrReceiver(clientId, pageRequest);

        return new PageDTO<>(
                transactions.getContent().stream().map(transactionMapper::toDTO).toList(),
                transactions
        );

    }
}
