package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.payload.CurrencyCreateDTO;
import uz.qarzdorlar_ai.payload.CurrencyDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.CurrencyService;

@RestController
@RequestMapping("/api/vi/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @PostMapping
    public ResponseEntity<ResponseDTO<CurrencyDTO>> createCurrency(@Valid @RequestBody CurrencyCreateDTO currencyCreateDTO){

        CurrencyDTO currencyDTO = currencyService.createCurrency(currencyCreateDTO);

        return ResponseEntity.ok(ResponseDTO.success(currencyDTO));

    }


    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<CurrencyDTO>> getByIdCurrency(@PathVariable Long id) {

        CurrencyDTO currencyDTO = currencyService.getByIdCurrency(id);

        return ResponseEntity.ok(ResponseDTO.success(currencyDTO));

    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<CurrencyDTO>>> getAllCurrency(@RequestParam(defaultValue = "0") Integer page,
                                                                            @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<CurrencyDTO> brandDTOPageDTO = currencyService.getAllCurrency(page, size);

        return ResponseEntity.ok(ResponseDTO.success(brandDTOPageDTO));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<CurrencyDTO>> updateCurrency(@PathVariable Long id,
                                                                   @Valid @RequestBody CurrencyCreateDTO currencyUpdateDTO) {

        CurrencyDTO brandDTO = currencyService.updateCurrency(id, currencyUpdateDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> deleteCurrency(@PathVariable Long id) {

        String deleteCurrency = currencyService.deleteCurrency(id);

        return ResponseEntity.ok(ResponseDTO.success(deleteCurrency));

    }


}
