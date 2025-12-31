package uz.qarzdorlar_ai.service.transactions.embedded;

import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;

public interface TransactionCreateService {

    TransactionDTO createTransaction(TransactionCreateDTO dto);

}
