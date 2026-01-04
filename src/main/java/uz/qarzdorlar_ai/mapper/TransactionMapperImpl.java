package uz.qarzdorlar_ai.mapper;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Transaction;
import uz.qarzdorlar_ai.model.TransactionItem;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.TransactionItemDTO;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionMapperImpl implements TransactionMapper {


    @Override
    public TransactionDTO toDTO(Transaction tx) {
        if (tx == null) {
            return null;
        }

        TransactionDTO dto = new TransactionDTO();

        dto.setId(tx.getId());
        dto.setCreatedAt(tx.getCreatedAt());
        dto.setType(tx.getType());
        dto.setStatus(tx.getStatus());
        dto.setDescription(tx.getDescription());

        if (tx.getClient() != null) {
            dto.setClientId(tx.getClient().getId());
            dto.setClientFullName(tx.getClient().getFullName());
            dto.setClientMainCurrency(tx.getClient().getCurrencyCode());
        }

        if (tx.getReceiverClient() != null) {
            dto.setReceiverId(tx.getReceiverClient().getId());
            dto.setReceiverFullName(tx.getReceiverClient().getFullName());
            dto.setReceiverMainCurrency(tx.getClient().getCurrencyCode());
        }

        dto.setTransactionCurrency(tx.getTransactionCurrency());
        dto.setAmount(tx.getAmount());
        dto.setRateToUsd(tx.getRateToUsd());

        dto.setUsdAmount(tx.getUsdAmount());
        dto.setBalanceEffect(tx.getBalanceEffect());
        dto.setClientRateSnapshot(tx.getClientRateSnapshot());

        dto.setReceiverBalanceEffect(dto.getReceiverBalanceEffect());
        dto.setReceiverRateSnapshot(dto.getReceiverRateSnapshot());

        dto.setFeeAmount(tx.getFeeAmount());
        dto.setCreatedByName(
                tx.getCreatedBy().getUserProfile().getFirstName() + " " +
                        tx.getCreatedBy().getUserProfile().getFirstName()
        );

        List<TransactionItemDTO> itemDTOS = getTransactionItemDTOS(tx);

        dto.setItems(itemDTOS);

        return dto;
    }

    private static List<TransactionItemDTO> getTransactionItemDTOS(Transaction tx) {

        if (tx.getItems() == null) {
            return new ArrayList<>();
        }

        List<TransactionItemDTO> itemDTOS = new ArrayList<>();
        for (TransactionItem item : tx.getItems()) {

            TransactionItemDTO txItemDTO = new TransactionItemDTO();
            txItemDTO.setProductId(item.getProduct().getId());
            txItemDTO.setProductName(item.getProduct().getName());
            txItemDTO.setQuantity(item.getQuantity());
            txItemDTO.setUnitPrice(item.getUnitPrice());
            txItemDTO.setTotalPrice(item.getTotalPrice());

            itemDTOS.add(txItemDTO);
        }
        return itemDTOS;
    }

}
