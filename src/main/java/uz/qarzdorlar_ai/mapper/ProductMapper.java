package uz.qarzdorlar_ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.payload.ProductCreateDTO;
import uz.qarzdorlar_ai.payload.ProductDTO;
import uz.qarzdorlar_ai.payload.ProductUpdateDTO;

import java.util.List;

//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    ProductDTO toDTO(Product product);

    List<ProductDTO> toDTO(List<Product> product);

    void setFieldProduct(Product product, ProductCreateDTO productCreateDTO);

    void setFieldProduct(Product product, ProductUpdateDTO productUpdateDTO);
}