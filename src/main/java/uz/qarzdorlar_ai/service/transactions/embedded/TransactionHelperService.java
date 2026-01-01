package uz.qarzdorlar_ai.service.transactions.embedded;

import uz.qarzdorlar_ai.model.Transaction;

public interface TransactionHelperService {
    void updateClientBalance(Transaction savedTransaction);

    void reverseClientBalance(Transaction tx);
}
