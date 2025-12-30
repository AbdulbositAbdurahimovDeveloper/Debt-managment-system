package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.payload.CurrencyCreateDTO;
import uz.qarzdorlar_ai.payload.CurrencyDTO;
import uz.qarzdorlar_ai.payload.PageDTO;

public interface CurrencyService {

    CurrencyDTO createCurrency(CurrencyCreateDTO currencyCreateDTO);

    CurrencyDTO getByIdCurrency(Long id);

    PageDTO<CurrencyDTO> getAllCurrency(Integer page, Integer size);

    CurrencyDTO updateCurrency(Long id, CurrencyCreateDTO currencyUpdateDTO);

    String deleteCurrency(Long id);

}
