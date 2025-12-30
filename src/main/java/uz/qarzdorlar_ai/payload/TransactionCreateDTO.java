package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Transaction}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCreateDTO implements Serializable {

    @NotNull
    private Long clientId;

    private Long receiverClientId;

    @NotNull
    private TransactionType type;

    private Long currencyId;

    private BigDecimal exchangeRate;

    private BigDecimal clientExchangeRate;

    private BigDecimal originalAmount;

    private BigDecimal receiverExchangeRate;

    private BigDecimal usdAmount;

    private BigDecimal balanceAmount;

    private BigDecimal feeAmount;

    private String description;

    private List<TransactionItemCreateDTO> items;

    private Timestamp createdAt;

}