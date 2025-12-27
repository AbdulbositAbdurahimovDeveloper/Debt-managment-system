package uz.qarzdorlar_ai.service;

import jakarta.validation.Valid;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.ProductCreateDTO;
import uz.qarzdorlar_ai.payload.ProductDTO;
import uz.qarzdorlar_ai.payload.ProductUpdateDTO;

import java.util.List;

public interface ProductService {

    ProductDTO createProduct( ProductCreateDTO productCreateDTO);

    ProductDTO getById(Long id);

    PageDTO<ProductDTO> getAllProducts(Integer page, Integer size);

    ProductDTO updateProduct(Long id, ProductUpdateDTO productUpdateDTO);

    String deleteProduct(Long id);
}
