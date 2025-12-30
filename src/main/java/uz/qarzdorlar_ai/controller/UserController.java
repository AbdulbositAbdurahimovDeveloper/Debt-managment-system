package uz.qarzdorlar_ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.user.UserDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<?>> getMe(@AuthenticationPrincipal User user){
        UserDTO userDTO = userService.getMe(user);
        return ResponseEntity.ok(ResponseDTO.success(userDTO));
    }
}
