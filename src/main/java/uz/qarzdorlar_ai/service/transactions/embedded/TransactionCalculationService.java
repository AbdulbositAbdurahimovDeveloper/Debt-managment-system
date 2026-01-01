package uz.qarzdorlar_ai.service.transactions.embedded;

import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;

public interface TransactionCalculationService {

    void calculateTransaction(TransactionCreateDTO dto, Transaction transaction, Client client);

}
