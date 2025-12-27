package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.qarzdorlar_ai.payload.user.LoginDTO;
import uz.qarzdorlar_ai.payload.TokenDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.payload.user.RegistrationResponseDTO;
import uz.qarzdorlar_ai.payload.user.UserRegisterRequestDTO;
import uz.qarzdorlar_ai.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<TokenDTO>> login(@Valid @RequestBody LoginDTO loginDTO) {

        TokenDTO tokenDTO = authService.login(loginDTO);

        return ResponseEntity.ok(ResponseDTO.success(tokenDTO));

    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<?>> register(@Valid @RequestBody UserRegisterRequestDTO registerDTO) {

        RegistrationResponseDTO requestDTO = authService.register(registerDTO);

        return ResponseEntity.ok(ResponseDTO.success(requestDTO));

    }
}
