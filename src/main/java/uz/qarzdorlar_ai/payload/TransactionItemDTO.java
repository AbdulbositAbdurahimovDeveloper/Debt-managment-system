package uz.qarzdorlar_ai.payload;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.qarzdorlar_ai.model.TransactionItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO for {@link TransactionItem}
 */
@Getter
@Setter
@NoArgsConstructor
public class TransactionItemDTO implements Serializable {
    private Long productId;
    private String productName; // Frontend uchun mahsulot nomi juda kerak
    private Integer quantity;
    private BigDecimal unitPrice; // USD da
    private BigDecimal totalPrice; // USD da
}