package uz.qarzdorlar_ai.service.transactions;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionHelperService;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransactionHelperServiceImpl implements TransactionHelperService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public void reverseClientBalance(Transaction tx) {
        Client client = tx.getClient();
        BigDecimal balanceEffect = tx.getBalanceEffect();

        // Diqqat: Bu metodda Switch-case yo'nalishlari updateClientBalance-ga nisbatan teskari bo'ladi
        switch (tx.getType()) {
            case SALE, RETURN_PAYMENT, TRANSFER ->
                // Sotuvda balans kamaygan edi, endi o'sha summani qaytarib qo'shamiz
                    client.setCurrentBalance(client.getCurrentBalance().add(balanceEffect));

            case PAYMENT, RETURN, PURCHASE ->
                // To'lovda balans oshgan edi, endi o'sha summani ayiramiz
                    client.setCurrentBalance(client.getCurrentBalance().subtract(balanceEffect));

            case PURCHASE_PAYMENT ->
                // Taminotchiga to'lovda balans kamaygan edi, endi qaytarib qo'shamiz
                    client.setCurrentBalance(client.getCurrentBalance().add(balanceEffect));
        }
        clientRepository.save(client);

        // TRANSFER bo'lsa, Receiver balansini ham orqaga qaytarish
        if (tx.getType() == TransactionType.TRANSFER && tx.getReceiverClient() != null) {
            Client receiver = tx.getReceiverClient();
            BigDecimal receiverSum = tx.getUsdAmount().multiply(tx.getReceiverRate());
            receiver.setCurrentBalance(receiver.getCurrentBalance().subtract(receiverSum));
            clientRepository.save(receiver);
        }
    }

    @Override
    @Transactional
    public void updateClientBalance(Transaction tx) {
        // 1. Asosiy mijozni olish (Client)
        Client client = tx.getClient();
        BigDecimal balanceEffect = tx.getBalanceEffect();

        // Null check: currentBalance null bo'lsa 0 deb olamiz
        if (client.getCurrentBalance() == null) {
            client.setCurrentBalance(client.getInitialBalance() != null ? client.getInitialBalance() : BigDecimal.ZERO);
        }

        // 2. ASOSIY MIJOZ BALANSINI YANGILASH
        // Mantiq: Balance > 0 bo'lsa biz qarzdormiz, Balance < 0 bo'lsa mijoz qarzdor
        switch (tx.getType()) {
            case SALE, RETURN_PAYMENT ->
                // Sotuvda mijozning qarzi oshadi (Balansi kamayadi)
                    client.setCurrentBalance(client.getCurrentBalance().subtract(balanceEffect));

            case PAYMENT, RETURN ->
                // To'lovda yoki vozvratda mijozning qarzi kamayadi (Balansi oshadi)
                    client.setCurrentBalance(client.getCurrentBalance().add(balanceEffect));

            case PURCHASE ->
                // Taminotchidan yuk oldik, uning bizdagi haqi oshdi (Balansi oshadi)
                    client.setCurrentBalance(client.getCurrentBalance().add(balanceEffect));

            case PURCHASE_PAYMENT ->
                // Taminotchiga pul berdik, bizning qarzimiz kamaydi (Balansi kamayadi)
                    client.setCurrentBalance(client.getCurrentBalance().subtract(balanceEffect));

            case TRANSFER ->
                // Pul o'tkazuvchi (Sender) balansi kamayadi
                    client.setCurrentBalance(client.getCurrentBalance().subtract(balanceEffect));
        }
        clientRepository.save(client);

        // 3. TRANSFER HOLATIDA OLUVCHI (RECEIVER) BALANSINI YANGILASH
        if (tx.getType() == TransactionType.TRANSFER && tx.getReceiverClient() != null) {
            updateReceiverBalance(tx);
        }
    }

    private void updateReceiverBalance(Transaction tx) {
        Client receiver = tx.getReceiverClient();

        if (receiver.getCurrentBalance() == null) {
            receiver.setCurrentBalance(receiver.getInitialBalance() != null ? receiver.getInitialBalance() : BigDecimal.ZERO);
        }

        // Oluvchi uchun balans o'zgarishi: usdAmount * receiverRate
        // Bu summa oluvchining o'z valyutasida (AED, UZS yoki USD) bo'ladi
        BigDecimal receiverEffect = tx.getUsdAmount().multiply(tx.getReceiverRate());

        // Oluvchining balansi har doim oshadi (chunki unga pul kirdi)
        receiver.setCurrentBalance(receiver.getCurrentBalance().add(receiverEffect));

        clientRepository.save(receiver);
    }
}
