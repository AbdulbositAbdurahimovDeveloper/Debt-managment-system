package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.ExchangeRate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO for {@link ExchangeRate}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateDTO implements Serializable {
    private Long id;
    private Long currencyId;
    private BigDecimal rate;
    private boolean deleted = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}