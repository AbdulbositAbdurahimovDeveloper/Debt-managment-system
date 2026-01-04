package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import uz.qarzdorlar_ai.model.Address;

import java.io.Serializable;

/**
 * DTO for {@link Address}
 */
@Getter
@Setter
public class AddressCreateDTO implements Serializable {

    @NotBlank(message = "Region must not be empty")
    private String region;

    @NotBlank(message = "District must not be empty")
    private String district;

    @Size(max = 255, message = "Street and house must not exceed 255 characters")
    private String streetAndHouse;

    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
    private Double longitude;

    @Size(max = 255, message = "Landmark must not exceed 255 characters")
    private String landmark;
}
