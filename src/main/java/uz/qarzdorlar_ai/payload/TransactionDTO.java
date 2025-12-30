package uz.qarzdorlar_ai.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.TransactionStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO implements Serializable {

    private Long id;

    private Long clientId;

    private Long receiverClientId;

    private Long userId;

    private TransactionType type;

    private TransactionStatus status;

    private Long currencyId;

    private BigDecimal exchangeRate;

    private BigDecimal clientExchangeRate;

    private BigDecimal originalAmount;

    private BigDecimal receiverExchangeRate;

    private BigDecimal usdAmount;

    private BigDecimal balanceAmount;

    private BigDecimal feeAmount;

    private String description;

    private List<TransactionItemDTO> items;

    private boolean deleted = false;

    private Timestamp createdAt;

    private Timestamp updatedAt;

}