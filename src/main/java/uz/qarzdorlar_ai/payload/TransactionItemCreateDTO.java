package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private Integer quantity;

    private BigDecimal unitPrice;

}