package uz.qarzdorlar_ai.service;

import jakarta.validation.Valid;
import uz.qarzdorlar_ai.payload.*;

import java.util.List;

public interface ProductService {

    ProductDTO createProduct( ProductCreateDTO productCreateDTO);

    ProductDTO getById(Long id);

    PageDTO<ProductDTO> getAllProducts(Integer page, Integer size);

    ProductDTO updateProduct(Long id, ProductUpdateDTO productUpdateDTO);

    String deleteProduct(Long id);

    PageDTO<ProductDTO> getSearchProducts(ProductFilterDTO productFilterDTO, Integer page, Integer size);
}
