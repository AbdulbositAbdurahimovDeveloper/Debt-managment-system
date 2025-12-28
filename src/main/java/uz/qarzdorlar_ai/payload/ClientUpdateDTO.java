package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.ClientType;

import java.io.Serializable;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Client}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientUpdateDTO implements Serializable {

    @NotBlank
    private String fullName;

    @NotBlank(message = "Phone number must not be empty")
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone number must contain only digits and may start with +"
    )
    private String phoneNumber;

    private ClientType type;

    @NotBlank(message = "Address must not be empty")
    private String address;

    @NotBlank(message = "Comment must not be empty")
    private String comment;

}