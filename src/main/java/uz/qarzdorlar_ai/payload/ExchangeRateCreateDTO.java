package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.ExchangeRate}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateCreateDTO implements Serializable {

    @NotNull
    private Long currencyId;

    @NotNull
    private BigDecimal rate;
}