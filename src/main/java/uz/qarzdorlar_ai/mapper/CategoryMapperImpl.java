package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.payload.CategoryDTO;

@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public CategoryDTO toDTO(Category category) {

        return new CategoryDTO(
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.isDeleted(),
                category.getId(),
                category.getName()
        );

    }
}
