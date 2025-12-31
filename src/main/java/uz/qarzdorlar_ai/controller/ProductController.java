package uz.qarzdorlar_ai.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.payload.*;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ResponseDTO<?>> createProduct(@Valid @RequestBody ProductCreateDTO productCreateDTO) {

        ProductDTO productDTO = productService.createProduct(productCreateDTO);

        return ResponseEntity.ok(ResponseDTO.success(productDTO));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> getById(@PathVariable Long id) {

        ProductDTO productDTO = productService.getById(id);

        return ResponseEntity.ok(ResponseDTO.success(productDTO));

    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<?>>> getAllProducts(@RequestParam(defaultValue = "0") Integer page,
                                                                  @RequestParam(defaultValue = "10") Integer size) {
        PageDTO<ProductDTO> productDTOListDTO = productService.getAllProducts(page, size);
        return ResponseEntity.ok(ResponseDTO.success(productDTOListDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> updateProduct(@PathVariable Long id,
                                                        @Valid @RequestBody ProductUpdateDTO productUpdateDTO) {

        ProductDTO productDTO = productService.updateProduct(id, productUpdateDTO);

        return ResponseEntity.ok(ResponseDTO.success(productDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> deleteProduct(@PathVariable Long id) {

        String deleteProduct = productService.deleteProduct(id);

        return ResponseEntity.ok(ResponseDTO.success(deleteProduct));

    }

    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<PageDTO<ProductDTO>>> getSearchProducts(ProductFilterDTO productFilterDTO,
                                                                              @RequestParam(defaultValue = "0") Integer page,
                                                                              @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<ProductDTO> products = productService.getSearchProducts(productFilterDTO,page,size);

        return ResponseEntity.ok(ResponseDTO.success(products));
    }

}
