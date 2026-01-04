package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.enums.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Transaction}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionUpdateDTO {

    Long clientId;
    Long receiverClientId;
    TransactionType type;
    CurrencyCode transactionCurrency;
    BigDecimal amount;
    BigDecimal rateToUsd;
    BigDecimal clientRateToUsd;
    BigDecimal receiverRateToUsd;
    BigDecimal feeAmount;
    String description;
    List<TransactionItemCreateDTO> items;
}

