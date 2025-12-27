package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.payload.user.LoginDTO;
import uz.qarzdorlar_ai.payload.TokenDTO;
import uz.qarzdorlar_ai.payload.user.RegistrationResponseDTO;
import uz.qarzdorlar_ai.payload.user.UserRegisterRequestDTO;

public interface AuthService {

    TokenDTO login(LoginDTO loginDTO);

    RegistrationResponseDTO register(UserRegisterRequestDTO registerDTO);


}
