package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilterDTO {

    private String search;

    private String username;

    private String firstName;
    private String lastName;

    private Role role;

    private String phoneNumber;

    private Boolean emailEnabled;

}

