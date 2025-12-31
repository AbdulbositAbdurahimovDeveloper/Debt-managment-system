package uz.qarzdorlar_ai.service.transactions.embedded;

import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.TransactionUpdateDTO;

public interface TransactionService {

    TransactionDTO createTransaction(TransactionCreateDTO dto, User staffUser);

    TransactionDTO updateTransaction(Long id, TransactionUpdateDTO dto, User staffUser);

    void deleteTransaction(Long id, User staffUser);

    TransactionDTO getByIdTransection(Long id);

    PageDTO<TransactionDTO> getByAllTransection(Integer page, Integer size);
}
