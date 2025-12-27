package uz.qarzdorlar_ai.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.UserDTO;

public interface UserService extends UserDetailsService {

    UserDTO getMe(User user);

}
