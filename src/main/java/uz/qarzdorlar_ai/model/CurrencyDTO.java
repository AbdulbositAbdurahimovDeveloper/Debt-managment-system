package uz.qarzdorlar_ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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