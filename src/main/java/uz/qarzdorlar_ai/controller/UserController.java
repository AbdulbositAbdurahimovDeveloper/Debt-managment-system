package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.payload.ProfileUpdateDTO;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.UserFilterDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.payload.user.UserDTO;
import uz.qarzdorlar_ai.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<?>> getMe(@AuthenticationPrincipal User user) {
        UserDTO userDTO = userService.getMe(user);
        return ResponseEntity.ok(ResponseDTO.success(userDTO));
    }

    @PutMapping("/me")
    public ResponseEntity<ResponseDTO<UserDTO>> updateUser(@RequestBody @Valid ProfileUpdateDTO dto,
                                                           @AuthenticationPrincipal User user) {

        UserDTO userDTO = userService.updateDTO(user, dto);

        return ResponseEntity.ok(ResponseDTO.success(userDTO));

    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','STAFF','STAFF_PLUS')")
    public ResponseEntity<ResponseDTO<PageDTO<UserDTO>>> getAllUser(@RequestParam(defaultValue = "0") Integer page,
                                                                    @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<UserDTO> userDTOPageDTO = userService.getAllUser(page, size);

        return ResponseEntity.ok(ResponseDTO.success(userDTOPageDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<String>> deleteUser(@PathVariable Long id,
                                                          @AuthenticationPrincipal User user) {

        userService.deleteUser(id, user);
        return ResponseEntity.ok(ResponseDTO.success("User deleted successfully"));

    }

    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<PageDTO<UserDTO>>> getSearchUser(UserFilterDTO userFilterDTO,
                                                                       @RequestParam(defaultValue = "0") Integer page,
                                                                       @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<UserDTO> result = userService.getSearch(userFilterDTO, page, size);

        return ResponseEntity.ok(ResponseDTO.success(result));
    }
}
