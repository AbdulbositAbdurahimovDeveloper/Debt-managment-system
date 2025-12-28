package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.payload.BrandDTO;
import uz.qarzdorlar_ai.payload.BrandCreateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;

public interface BrandService {

    BrandDTO createBrand(BrandCreateDTO brandCreateDTO);

    BrandDTO getByIdBrand(Long id);

    PageDTO<BrandDTO> getAllBrands(Integer page, Integer size);

    BrandDTO updateBrand(Long id, BrandCreateDTO brandUpdateDTO);

    String deleteBrand(Long id);

}
