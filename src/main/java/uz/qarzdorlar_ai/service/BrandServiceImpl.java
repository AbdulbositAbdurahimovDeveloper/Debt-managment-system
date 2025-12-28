package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.BrandMapper;
import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.payload.BrandDTO;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.BrandCreateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.repository.BrandRepository;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    public BrandDTO createBrand(BrandCreateDTO brandCreateDTO) {

        Brand brand = new Brand();
        brand.setName(brandCreateDTO.name());

        brandRepository.save(brand);

        return brandMapper.toDTO(brand);
    }

    @Override
    public BrandDTO getByIdBrand(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with id : " + id));

        return brandMapper.toDTO(brand);
    }

    @Override
    public PageDTO<BrandDTO> getAllBrands(Integer page, Integer size) {

        Sort sort = Sort.by(AbsDateEntity.Fields.createdAt);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Brand> brandPage = brandRepository.findAll(pageRequest);

        return new PageDTO<>(
                brandPage.getContent().stream().map(brandMapper::toDTO).toList(),
                brandPage
        );
    }

    @Override
    public BrandDTO updateBrand(Long id, BrandCreateDTO brandUpdateDTO) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with id : " + id));

        brand.setName(brandUpdateDTO.name());

        brandRepository.save(brand);

        return brandMapper.toDTO(brand);
    }

    @Override
    public String deleteBrand(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with id : " + id));

        brandRepository.delete(brand);

        return "Brand deleted successfully. Deleted brand with id : " + brand.getId();
    }
}
