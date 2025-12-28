package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.payload.BrandDTO;
import uz.qarzdorlar_ai.payload.BrandCreateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.BrandService;


@RestController
@RequestMapping("/api/v1/brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<ResponseDTO<BrandDTO>> createBrand(@Valid @RequestBody BrandCreateDTO brandCreateDTO) {

        BrandDTO brandDTO = brandService.createBrand(brandCreateDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<BrandDTO>> getByIdBrand(@PathVariable Long id) {

        BrandDTO brandDTO = brandService.getByIdBrand(id);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<BrandDTO>>> getAllBrands(@RequestParam(defaultValue = "0") Integer page,
                                                                       @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<BrandDTO> brandDTOPageDTO = brandService.getAllBrands(page, size);

        return ResponseEntity.ok(ResponseDTO.success(brandDTOPageDTO));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<BrandDTO>> updateBrand(@PathVariable Long id,
                                                             @Valid @RequestBody BrandCreateDTO brandUpdateDTO) {

        BrandDTO brandDTO = brandService.updateBrand(id, brandUpdateDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> deleteBrand(@PathVariable Long id){

        String deleteBrand = brandService.deleteBrand(id);

        return ResponseEntity.ok(ResponseDTO.success(deleteBrand));

    }

}
