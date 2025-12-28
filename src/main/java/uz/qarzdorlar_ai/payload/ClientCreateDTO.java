package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.ClientType;
import uz.qarzdorlar_ai.model.Client;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link Client}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientCreateDTO implements Serializable {

    @NotNull(message = "Full name must not be null")
    @NotBlank(message = "Full name must not be empty")
    private String fullName;

    @NotBlank(message = "Phone number must not be empty")
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone number must contain only digits and may start with +"
    )
    private String phoneNumber;

    @NotNull(message = "Client type is required")
    private ClientType type;

    @NotNull(message = "Currency id required")
    private Long currencyId;

    @PositiveOrZero(message = "Initial balance must be zero or a positive number")
    private BigDecimal initialBalance;

    @NotBlank(message = "Address must not be empty")
    private String address;

    @NotBlank(message = "Comment must not be empty")
    private String comment;

}
