package uz.qarzdorlar_ai.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import uz.qarzdorlar_ai.payload.ProfileUpdateDTO;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.UserFilterDTO;
import uz.qarzdorlar_ai.payload.user.UserDTO;

public interface UserService extends UserDetailsService {

    UserDTO getMe(User user);

    UserDTO updateDTO(User user, ProfileUpdateDTO dto);

    PageDTO<UserDTO> getAllUser(Integer page, Integer size);

    void deleteUser(Long id, User user);

    PageDTO<UserDTO> getSearch(UserFilterDTO userFilterDTO, Integer page, Integer size);

}
