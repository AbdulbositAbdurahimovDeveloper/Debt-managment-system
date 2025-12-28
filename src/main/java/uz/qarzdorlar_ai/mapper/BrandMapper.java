package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.payload.BrandDTO;

public interface BrandMapper {

    BrandDTO toDTO(Brand brand);


}
