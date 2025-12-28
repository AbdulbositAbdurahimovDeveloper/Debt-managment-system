package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.payload.CategoryCreateUpdateDTO;
import uz.qarzdorlar_ai.payload.CategoryDTO;
import uz.qarzdorlar_ai.payload.PageDTO;

public interface CategoryService {
    CategoryDTO createCategory(CategoryCreateUpdateDTO categoryCreateUpdateDTO);

    CategoryDTO getByIdCategory(Long id);

    PageDTO<CategoryDTO> getAllCategory(Integer page, Integer size);

    CategoryDTO updateCategory(Long id, CategoryCreateUpdateDTO categoryCreateUpdateDTO);

    String deleteCategory(Long id);
}
