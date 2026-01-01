package uz.qarzdorlar_ai.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.TransactionItemDTO;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionMapperImpl implements TransactionMapper {


    @Override
    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionDTO dto = new TransactionDTO();

        dto.setId(transaction.getId());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());

        // IDs (Null-safe)
        if (transaction.getClient() != null) {
            dto.setClientId(transaction.getClient().getId());
        }
        if (transaction.getReceiverClient() != null) {
            dto.setReceiverClientId(transaction.getReceiverClient().getId());
        }
        if (transaction.getCreatedBy() != null) {
            dto.setUserId(transaction.getCreatedBy().getId());
        }

        // Valyuta va Kurslar
        dto.setTransactionCurrency(transaction.getTransactionCurrency());
        dto.setAmount(transaction.getAmount());
        dto.setMarketRate(transaction.getMarketRate());

        // USD Pivot
        dto.setUsdAmount(transaction.getUsdAmount());
        dto.setFeeAmount(transaction.getFeeAmount());

        // Client Balansi Snapshot
        dto.setClientCurrency(transaction.getClientCurrency());
        dto.setClientRate(transaction.getClientRate());
        dto.setBalanceEffect(transaction.getBalanceEffect());

        // Transfer uchun
        dto.setReceiverRate(transaction.getReceiverRate());

        dto.setDescription(transaction.getDescription());

        // Items mapping
        if (transaction.getItems() != null) {
            dto.setItems(transaction.getItems().stream()
                    .map(item -> {
                        TransactionItemDTO itemDto = new TransactionItemDTO();
                        itemDto.setId(item.getId());
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setUnitPrice(item.getUnitPrice());
                        itemDto.setTotalPrice(item.getTotalPrice());
                        itemDto.setCreatedAt(item.getCreatedAt());
                        itemDto.setUpdatedAt(item.getUpdatedAt());
                        if (item.getProduct() != null) {
                            itemDto.setProductId(item.getProduct().getId());
                            itemDto.setProductName(item.getProduct().getName());
                        }
                        return itemDto;
                    })
                    .toList());
        } else {
            dto.setItems(List.of());
        }

        dto.setDeleted(transaction.isDeleted());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());

        return dto;
    }

}
