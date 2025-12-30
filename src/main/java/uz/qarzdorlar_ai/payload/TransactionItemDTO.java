package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.TransactionItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO for {@link TransactionItem}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionItemDTO implements Serializable {
    private Long id;
    private Long transactionId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private boolean deleted = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}