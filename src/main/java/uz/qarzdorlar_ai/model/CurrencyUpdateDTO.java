package uz.qarzdorlar_ai.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for {@link Currency}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyUpdateDTO implements Serializable {

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotBlank
    private String symbol;

    private boolean isBase;

}