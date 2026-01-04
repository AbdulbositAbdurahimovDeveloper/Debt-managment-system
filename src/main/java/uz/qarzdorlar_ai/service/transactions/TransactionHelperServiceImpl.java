package uz.qarzdorlar_ai.service.transactions;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.exception.BadRequestException;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionHelperService;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class TransactionHelperServiceImpl implements TransactionHelperService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public void reverseClientBalance(Transaction tx) {
        // 1. ASOSIY MIJOZ BALANSINI QAYTARISH
        // Qoida: Bazada nima bo'lsa, shuning teskarisini (-1 ga ko'paytirilganini) yuboramiz
        if (tx.getBalanceEffect() != null) {
            BigDecimal reverseEffect = tx.getBalanceEffect().negate();
            clientRepository.updateBalance(tx.getClient().getId(), reverseEffect);
        }

        // 2. TRANSFER HOLATIDA RECEIVER BALANSINI QAYTARISH
        // Qoida: Receiverga qo'shilgan summani endi ayirib tashlaymiz
        if (tx.getType() == TransactionType.TRANSFER && tx.getReceiverClient() != null) {
            if (tx.getReceiverBalanceEffect() != null) {
                BigDecimal reverseReceiverEffect = tx.getReceiverBalanceEffect().negate();
                clientRepository.updateBalance(tx.getReceiverClient().getId(), reverseReceiverEffect);
            }
        }
    }

    @Override
    @Transactional
    public void updateClientBalance(Transaction tx) {
        // Create vaqtida borini boricha qo'shamiz (ishoralari ichida bor)

        // Asosiy mijozga
        clientRepository.updateBalance(tx.getClient().getId(), tx.getBalanceEffect());

        // Agar transfer bo'lsa, qabul qiluvchiga
        if (tx.getType() == TransactionType.TRANSFER && tx.getReceiverClient() != null) {
            clientRepository.updateBalance(tx.getReceiverClient().getId(), tx.getReceiverBalanceEffect());
        }
    }
}
