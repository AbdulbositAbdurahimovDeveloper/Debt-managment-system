package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uz.qarzdorlar_ai.model.Category;

import java.io.Serializable;

/**
 * DTO for {@link Category}
 */
public record CategoryCreateUpdateDTO(@NotNull @NotBlank String name) implements Serializable {
}