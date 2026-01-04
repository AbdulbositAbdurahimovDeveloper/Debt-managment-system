package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.UserProfile;

import java.io.Serializable;

/**
 * DTO for {@link UserProfile}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileUpdateDTO implements Serializable {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}