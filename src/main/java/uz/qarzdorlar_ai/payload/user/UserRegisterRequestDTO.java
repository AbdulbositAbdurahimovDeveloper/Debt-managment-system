package uz.qarzdorlar_ai.payload.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user registration requests.
 * Contains all the necessary information provided by the user to create a new account.
 * Includes validation annotations to ensure data integrity before processing.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequestDTO {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between {min} and {max} characters")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 4, message = "Password must be at least {min} characters long")
    private String password;

    @NotBlank(message = "First name must not be blank")
    @Size(min = 2, max = 50, message = "First name must be between {min} and {max} characters")
    private String firstName;

//    @NotBlank(message = "Last name must not be blank")
    @Size(min = 2, max = 50, message = "Last name must be between {min} and {max} characters")
    private String lastName;

//    @NotBlank(message = "Email must not be blank")
    @Email(message = "Please provide a valid email address")
    private String email;

    // Telefon raqami majburiy bo'lmagani uchun @NotBlank qo'yilmaydi
    private String phoneNumber;
}