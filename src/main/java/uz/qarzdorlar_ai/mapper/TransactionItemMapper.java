package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.TransactionItem;
import uz.qarzdorlar_ai.payload.TransactionItemDTO;

public interface TransactionItemMapper {

    TransactionItemDTO toDTO(TransactionItem transactionItem);

}
