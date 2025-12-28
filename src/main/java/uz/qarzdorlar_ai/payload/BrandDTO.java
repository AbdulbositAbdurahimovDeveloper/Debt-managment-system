package uz.qarzdorlar_ai.payload;

import uz.qarzdorlar_ai.model.Brand;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO for {@link Brand}
 */
public record BrandDTO(
        Timestamp createdAt,
        Timestamp updatedAt,
        Long id,
        String name
) implements Serializable {
}