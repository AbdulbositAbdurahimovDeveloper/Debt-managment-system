package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.Address;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO for {@link Address}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO implements Serializable {
    private Timestamp createdAt;
    private Long id;
    private String region;
    private String district;
    private String streetAndHouse;
    private Double latitude;
    private Double longitude;
    private String landmark;
}