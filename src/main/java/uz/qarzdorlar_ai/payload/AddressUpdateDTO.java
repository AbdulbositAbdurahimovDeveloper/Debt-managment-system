package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AddressUpdateDTO implements Serializable {

    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;

    @Size(max = 100, message = "District must not exceed 100 characters")
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
