package uz.qarzdorlar_ai.payload;

import uz.qarzdorlar_ai.model.Category;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO for {@link Category}
 */
public record CategoryDTO(
        Timestamp createdAt,
        Timestamp updatedAt,
        boolean deleted,
        Long id,
        String name
) implements Serializable {
}