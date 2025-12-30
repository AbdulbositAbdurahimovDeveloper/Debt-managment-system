package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.payload.TransactionDTO;

public interface TransactionMapper {
    TransactionDTO toDTO(Transaction savedTransaction);

}
