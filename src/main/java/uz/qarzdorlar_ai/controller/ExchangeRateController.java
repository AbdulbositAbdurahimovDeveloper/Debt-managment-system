package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.payload.ExchangeRateDTO;
import uz.qarzdorlar_ai.payload.ExchangeRateCreateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.ExchangeRateService;

@RestController
@RequestMapping("/api/v1/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @PostMapping
    public ResponseEntity<ResponseDTO<ExchangeRateDTO>> post(@RequestBody @Valid ExchangeRateCreateDTO exchangeRateCreateDTO) {

        ExchangeRateDTO exchangeRateDTO = exchangeRateService.createExchangeRate(exchangeRateCreateDTO);

        return ResponseEntity.ok(ResponseDTO.success(exchangeRateDTO));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<ExchangeRateDTO>> getByIdExchangeRate(@PathVariable Long id) {

        ExchangeRateDTO exchangeRateDTO = exchangeRateService.getByIdExchangeRate(id);

        return ResponseEntity.ok(ResponseDTO.success(exchangeRateDTO));

    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<ExchangeRateDTO>>> getAllExchangeRate(@RequestParam(defaultValue = "0") Integer page,
                                                                                    @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<ExchangeRateDTO> exchangeRateDTOPageDTO = exchangeRateService.getAllExchangeRate(page, size);

        return ResponseEntity.ok(ResponseDTO.success(exchangeRateDTOPageDTO));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<ExchangeRateDTO>> updateExchangeRate(@PathVariable Long id,@Valid @RequestBody ExchangeRateCreateDTO createDTO) {

        ExchangeRateDTO exchangeRateDTO = exchangeRateService.updateExchangeRate(id,createDTO);

        return ResponseEntity.ok(ResponseDTO.success(exchangeRateDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<String>> deleteExchangeRate(@PathVariable Long id) {

        String response = exchangeRateService.deleteExchangeRate(id);

        return ResponseEntity.ok(ResponseDTO.success(response));

    }

}
