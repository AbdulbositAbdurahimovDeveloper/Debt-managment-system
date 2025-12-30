package uz.qarzdorlar_ai.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.payload.TransactionDTO;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionMapperImpl implements TransactionMapper {

    private final TransactionItemMapper mapper;

    @Override
    public TransactionDTO toDTO(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        TransactionDTO dto = new TransactionDTO();

        dto.setId(transaction.getId());

        dto.setClientId(
                transaction.getClient() != null
                        ? transaction.getClient().getId()
                        : null
        );

        dto.setReceiverClientId(
                transaction.getReceiverClient() != null
                        ? transaction.getReceiverClient().getId()
                        : null
        );

        dto.setUserId(
                transaction.getUser() != null
                        ? transaction.getUser().getId()
                        : null
        );

        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());

        dto.setCurrencyId(
                transaction.getCurrency() != null
                        ? transaction.getCurrency().getId()
                        : null
        );

        dto.setExchangeRate(transaction.getExchangeRate());
        dto.setOriginalAmount(transaction.getOriginalAmount());
        dto.setReceiverExchangeRate(transaction.getReceiverExchangeRate());
        dto.setUsdAmount(transaction.getUsdAmount());
        dto.setBalanceAmount(transaction.getBalanceAmount());
        dto.setFeeAmount(transaction.getFeeAmount());
        dto.setDescription(transaction.getDescription());

        dto.setItems(
                transaction.getItems() != null
                        ? transaction.getItems().stream()
                        .map(mapper::toDTO)
                        .toList()
                        : List.of()
        );

        dto.setDeleted(transaction.isDeleted());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());

        return dto;
    }

}
