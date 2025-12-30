package uz.qarzdorlar_ai.payload.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.Role;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.User}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO implements Serializable {

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Long id;
    private String username;
    private Role role = Role.USER;
    private Long telegramUserChatId;
    private Long userProfileId;
    private String userProfileFirstName;
    private String userProfileLastName;
    private String userProfileEmail;
    private String userProfilePhoneNumber;
    private boolean userProfileDeleted = false;
    private boolean userProfileEmailEnabled = false;
    private boolean deleted = false;

}