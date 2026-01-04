package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.user.UserDTO;

public interface UserMapper {

    UserDTO toDTO(User user);

}
