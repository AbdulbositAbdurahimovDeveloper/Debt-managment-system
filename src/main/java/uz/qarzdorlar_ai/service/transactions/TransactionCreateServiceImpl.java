package uz.qarzdorlar_ai.service.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionCreateService;

@Service
@RequiredArgsConstructor
public class TransactionCreateServiceImpl implements TransactionCreateService {

    @Override
    public TransactionDTO createTransaction(TransactionCreateDTO dto) {
        return null;
    }
}
