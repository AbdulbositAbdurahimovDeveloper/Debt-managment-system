package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.payload.ExchangeRateDTO;
import uz.qarzdorlar_ai.payload.ExchangeRateCreateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;

public interface ExchangeRateService {

    ExchangeRateDTO createExchangeRate(ExchangeRateCreateDTO exchangeRateCreateDTO);

    ExchangeRateDTO getByIdExchangeRate(Long id);

    PageDTO<ExchangeRateDTO> getAllExchangeRate(Integer page, Integer size);

    ExchangeRateDTO updateExchangeRate(Long id, ExchangeRateCreateDTO createDTO);

    String deleteExchangeRate(Long id);

}
