package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.TransactionItem}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionItemCreateDTO implements Serializable {

    @NotNull
    private Long productId;

    @Positive
    private Integer quantity;

    @PositiveOrZero
    private BigDecimal unitPrice;

}