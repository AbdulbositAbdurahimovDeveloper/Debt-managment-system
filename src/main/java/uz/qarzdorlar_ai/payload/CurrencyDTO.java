package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.Currency;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO for {@link Currency}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyDTO implements Serializable {
    private Long id;
    private String name;
    private String code;
    private String symbol;
    private boolean isBase;
    private boolean deleted = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}