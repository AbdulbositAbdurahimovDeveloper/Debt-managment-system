package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.payload.BrandDTO;

@Component
public class BrandMapperImpl implements BrandMapper {

    @Override
    public BrandDTO toDTO(Brand brand) {
        return new BrandDTO(
                brand.getCreatedAt(),
                brand.getUpdatedAt(),
                brand.getId(),
                brand.getName()
        );
    }
}
