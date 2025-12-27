package uz.qarzdorlar_ai.payload.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ErrorDTO {

    private int status;
    private String message;
    private List<FieldErrorDTO> fieldErrors;

    public ErrorDTO(int status, String message) {
        this.status = status;
        this.message = message;
    }
}