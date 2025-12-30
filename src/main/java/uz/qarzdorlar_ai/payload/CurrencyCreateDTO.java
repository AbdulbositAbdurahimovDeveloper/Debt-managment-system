package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.Currency;

import java.io.Serializable;

/**
 * DTO for {@link Currency}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyCreateDTO implements Serializable {

    @NotNull
    @NotBlank
    private String name;

    @NotNull
    @NotBlank

    private String code;

    @NotNull
    @NotBlank
    private String symbol;

    private boolean isBase;

}