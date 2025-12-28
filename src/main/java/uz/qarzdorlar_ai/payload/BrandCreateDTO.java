package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Brand}
 */
public record BrandCreateDTO(@NotNull @NotBlank String name) implements Serializable {
}