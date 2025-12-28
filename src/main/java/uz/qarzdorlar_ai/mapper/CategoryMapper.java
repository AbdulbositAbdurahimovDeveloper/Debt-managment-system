package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.payload.CategoryDTO;

public interface CategoryMapper {

    CategoryDTO toDTO(Category category);

}
