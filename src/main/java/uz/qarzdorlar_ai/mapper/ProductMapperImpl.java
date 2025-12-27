package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.payload.ProductCreateDTO;
import uz.qarzdorlar_ai.payload.ProductDTO;
import uz.qarzdorlar_ai.payload.ProductUpdateDTO;

import java.util.List;

@Component
public class ProductMapperImpl implements ProductMapper {


    @Override
    public ProductDTO toDTO(Product product) {

        Brand brand = product.getBrand();
        String brandName = brand.getName();

        Category category = product.getCategory();
        String categoryName = category.getName();

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(product.getId());
        productDTO.setName(product.getName());
        productDTO.setBrandName(brandName);
        productDTO.setCategoryName(categoryName);
        productDTO.setPrice(product.getPriceUsd());
        productDTO.setCpu(product.getCpu());
        productDTO.setRam(product.getRam());
        productDTO.setStorage(product.getStorage());
        productDTO.setGpu(product.getGpu());
        productDTO.setDisplay(product.getDisplay());
        productDTO.setResolution(product.getResolution());
        productDTO.setOs(product.getOs());
        productDTO.setColor(product.getColor());
        productDTO.setModelCode(productDTO.getModelCode());
        productDTO.setTouchScreen(product.getTouchScreen());
        productDTO.setBacklit(product.getBacklit());
        productDTO.setDescription(productDTO.getDescription());
        productDTO.setRawData(product.getRawData());
        productDTO.setCreatedAt(product.getCreatedAt());
        productDTO.setUpdatedAt(product.getUpdatedAt());

        return productDTO;
    }

    @Override
    public List<ProductDTO> toDTO(List<Product> product) {
       return product.stream().map(this::toDTO).toList();
    }

    @Override
    public void setFieldProduct(Product product, ProductCreateDTO productCreateDTO) {
        product.setName(productCreateDTO.getName());
        product.setPriceUsd(productCreateDTO.getPriceUsd());
        product.setCpu(productCreateDTO.getCpu());
        product.setRam(productCreateDTO.getRam());
        product.setStorage(productCreateDTO.getStorage());
        product.setGpu(productCreateDTO.getGpu());
        product.setDisplay(productCreateDTO.getDisplay());
        product.setResolution(productCreateDTO.getResolution());
        product.setOs(productCreateDTO.getOs());
        product.setColor(productCreateDTO.getColor());
        product.setModelCode(productCreateDTO.getModelCode());
        product.setTouchScreen(productCreateDTO.getTouchScreen());
        product.setBacklit(productCreateDTO.getBacklit());
        product.setDescription(productCreateDTO.getDescription());
        product.setRawData(productCreateDTO.getRawData());

    }

    @Override
    public void setFieldProduct(Product product, ProductUpdateDTO productUpdateDTO) {
        if (productUpdateDTO.getName() != null)
            product.setName(productUpdateDTO.getName());

        if (productUpdateDTO.getPriceUsd() != null)
            product.setPriceUsd(productUpdateDTO.getPriceUsd());

        if (productUpdateDTO.getCpu() != null)
            product.setCpu(productUpdateDTO.getCpu());

        if (productUpdateDTO.getRam() != null)
            product.setRam(productUpdateDTO.getRam());

        if (productUpdateDTO.getStorage() != null)
            product.setStorage(productUpdateDTO.getStorage());

        if (productUpdateDTO.getGpu() != null)
            product.setGpu(productUpdateDTO.getGpu());

        if (productUpdateDTO.getDisplay() != null)
            product.setDisplay(productUpdateDTO.getDisplay());

        if (productUpdateDTO.getResolution() != null)
            product.setResolution(productUpdateDTO.getResolution());

        if (productUpdateDTO.getOs() != null)
            product.setOs(productUpdateDTO.getOs());

        if (productUpdateDTO.getColor() != null)
            product.setColor(productUpdateDTO.getColor());

        if (productUpdateDTO.getModelCode() != null)
            product.setModelCode(productUpdateDTO.getModelCode());

        if (productUpdateDTO.getTouchScreen() != null)
            product.setTouchScreen(productUpdateDTO.getTouchScreen());

        if (productUpdateDTO.getBacklit() != null)
            product.setBacklit(productUpdateDTO.getBacklit());

        if (productUpdateDTO.getDescription() != null)
            product.setDescription(productUpdateDTO.getDescription());

        if (productUpdateDTO.getRawData() != null)
            product.setRawData(productUpdateDTO.getRawData());
    }
}
