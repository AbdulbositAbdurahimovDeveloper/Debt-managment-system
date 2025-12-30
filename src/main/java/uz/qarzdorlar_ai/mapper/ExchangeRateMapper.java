package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.ExchangeRate;
import uz.qarzdorlar_ai.payload.ExchangeRateDTO;

public interface ExchangeRateMapper {
    ExchangeRateDTO toDTO(ExchangeRate exchangeRate);
}
