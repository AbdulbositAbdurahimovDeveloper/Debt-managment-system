package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.ProductMapper;
import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.*;
import uz.qarzdorlar_ai.repository.BrandRepository;
import uz.qarzdorlar_ai.repository.CategoryRepository;
import uz.qarzdorlar_ai.repository.ProductRepository;
import uz.qarzdorlar_ai.specification.ProductSpecification;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductCreateDTO productCreateDTO) {

        Long brandId = productCreateDTO.getBrandId();
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with id : " + brandId));

        Long categoryId = productCreateDTO.getCategoryId();
        Category category = categoryRepository.findById(categoryId).
                orElseThrow(() -> new EntityNotFoundException("Category not found with id : " + categoryId));

        Product product = new Product();
        product.setBrand(brand);
        product.setCategory(category);

        productMapper.setFieldProduct(product, productCreateDTO);

        productRepository.save(product);

        return productMapper.toDTO(product);

    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id : " + id));

        return productMapper.toDTO(product);

    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ProductDTO> getAllProducts(Integer page, Integer size) {

        Sort sort = Sort.by(AbsDateEntity.Fields.createdAt);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Product> products = productRepository.findAll(pageRequest);

        List<ProductDTO> productDTOList = productMapper.toDTO(products.getContent());

        return new PageDTO<>(
                productDTOList,
                products
        );
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, ProductUpdateDTO productUpdateDTO) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id : " + id));

        if (productUpdateDTO.getBrandId() != null) {
            Long brandId = productUpdateDTO.getBrandId();
            Brand brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("Brand not found with id : " + brandId));

            product.setBrand(brand);
        }

        if (productUpdateDTO.getCategoryId() != null) {
            Long categoryId = productUpdateDTO.getCategoryId();
            Category category = categoryRepository.findById(categoryId).
                    orElseThrow(() -> new EntityNotFoundException("Category not found with id : " + categoryId));

            product.setCategory(category);
        }

        productMapper.setFieldProduct(product, productUpdateDTO);

        productRepository.save(product);

        return productMapper.toDTO(product);

    }

    @Override
    @Transactional
    public String deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id : " + id));

        productRepository.delete(product);

        return "Product deleted successfully. Deleted product with id : " + product.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ProductDTO> getSearchProducts(ProductFilterDTO productFilterDTO, Integer page, Integer size) {
        // 1. Sortirovkani (masalan, yaratilgan vaqti bo'yicha) sozlash
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // 2. Specification orqali qidiruvni amalga oshirish
        Specification<Product> spec = ProductSpecification.build(productFilterDTO);
        Page<Product> productsPage = productRepository.findAll(spec, pageRequest);

        // 3. DTOga o'girish
        List<ProductDTO> productDTOList = productMapper.toDTO(productsPage.getContent());

        // 4. Siz so'ragan PageDTO formatida qaytarish
        return new PageDTO<>(
                productDTOList,
                productsPage
        );
    }
}
