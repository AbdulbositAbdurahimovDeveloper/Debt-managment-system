package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.TransactionItem;
import uz.qarzdorlar_ai.payload.TransactionItemDTO;

@Component
public class TransactionItemMapperImpl implements TransactionItemMapper {

    @Override
    public TransactionItemDTO toDTO(TransactionItem transactionItem) {

        if (transactionItem == null) {
            return null;
        }

        TransactionItemDTO dto = new TransactionItemDTO();

        dto.setId(transactionItem.getId());

        dto.setTransactionId(
                transactionItem.getTransaction() != null
                        ? transactionItem.getTransaction().getId()
                        : null
        );

        dto.setProductId(
                transactionItem.getProduct() != null
                        ? transactionItem.getProduct().getId()
                        : null
        );

        dto.setQuantity(transactionItem.getQuantity());
        dto.setUnitPrice(transactionItem.getUnitPrice());
        dto.setTotalPrice(transactionItem.getTotalPrice());

        dto.setDeleted(transactionItem.isDeleted());
        dto.setCreatedAt(transactionItem.getCreatedAt());
        dto.setUpdatedAt(transactionItem.getUpdatedAt());

        return dto;
    }

}
