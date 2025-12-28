package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.payload.CategoryCreateUpdateDTO;
import uz.qarzdorlar_ai.payload.CategoryDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.CategoryService;

@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ResponseDTO<CategoryDTO>> createBrand(@Valid @RequestBody CategoryCreateUpdateDTO categoryCreateDTO) {

        CategoryDTO brandDTO = categoryService.createCategory(categoryCreateDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<CategoryDTO>> getByIdCategory(@PathVariable Long id) {

        CategoryDTO brandDTO = categoryService.getByIdCategory(id);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<CategoryDTO>>> getAllCategory(@RequestParam(defaultValue = "0") Integer page,
                                                                            @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<CategoryDTO> brandDTOPageDTO = categoryService.getAllCategory(page, size);

        return ResponseEntity.ok(ResponseDTO.success(brandDTOPageDTO));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<CategoryDTO>> updateCategory(@PathVariable Long id,
                                                                   @Valid @RequestBody CategoryCreateUpdateDTO categoryDTO) {

        CategoryDTO brandDTO = categoryService.updateCategory(id, categoryDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> deleteCategory(@PathVariable Long id) {

        String deleteCategory = categoryService.deleteCategory(id);

        return ResponseEntity.ok(ResponseDTO.success(deleteCategory));

    }


}
