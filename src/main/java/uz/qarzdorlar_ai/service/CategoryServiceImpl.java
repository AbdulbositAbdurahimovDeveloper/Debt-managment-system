package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.CategoryMapper;
import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.payload.CategoryCreateUpdateDTO;
import uz.qarzdorlar_ai.payload.CategoryDTO;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.repository.CategoryRepository;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDTO createCategory(CategoryCreateUpdateDTO categoryCreateUpdateDTO) {

        Category category = new Category();
        category.setName(categoryCreateUpdateDTO.name());

        categoryRepository.save(category);

        return categoryMapper.toDTO(category);
    }

    @Override
    public CategoryDTO getByIdCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id : " + id));

        return categoryMapper.toDTO(category);
    }

    @Override
    public PageDTO<CategoryDTO> getAllCategory(Integer page, Integer size) {

        Sort sort = Sort.by(AbsDateEntity.Fields.createdAt);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Category> categoryPage = categoryRepository.findAll(pageRequest);

        return new PageDTO<>(
                categoryPage.getContent().stream().map(categoryMapper::toDTO).toList(),
                categoryPage
        );
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryCreateUpdateDTO categoryCreateUpdateDTO) {


        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id : " + id));


        category.setName(categoryCreateUpdateDTO.name());

        categoryRepository.save(category);

        return categoryMapper.toDTO(category);

    }

    @Override
    public String deleteCategory(Long id) {


        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id : " + id));

        categoryRepository.delete(category);

        return "Category deleted successfully. Deleted Category with id : " + category.getId();
    }
}
