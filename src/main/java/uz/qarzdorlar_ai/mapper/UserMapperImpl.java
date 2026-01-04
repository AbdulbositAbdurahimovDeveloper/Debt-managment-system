package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.model.UserProfile;
import uz.qarzdorlar_ai.payload.user.UserDTO;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDTO toDTO(User user) {
        if (user == null)
            return null;

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole());
        userDTO.setTelegramUserChatId(user.getTelegramUser() != null
                ? user.getTelegramUser().getChatId()
                : null
        );
        UserProfile profile = user.getUserProfile();
        if (profile != null){

            userDTO.setUserProfileId(profile.getId());
            userDTO.setUserProfileFirstName(profile.getFirstName());
            userDTO.setUserProfileLastName(profile.getLastName());
            userDTO.setUserProfileEmail(profile.getEmail());
            userDTO.setUserProfilePhoneNumber(profile.getPhoneNumber());
            userDTO.setUserProfileDeleted(profile.isDeleted());
            userDTO.setUserProfileEmailEnabled(profile.isEmailEnabled());
            userDTO.setDeleted(profile.isDeleted());

        }
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());

        return userDTO;

    }
}
