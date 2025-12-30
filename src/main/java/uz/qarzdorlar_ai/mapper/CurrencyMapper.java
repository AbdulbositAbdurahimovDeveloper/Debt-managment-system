package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.payload.CurrencyDTO;

public interface CurrencyMapper {

    CurrencyDTO toDTO(Currency currency);

}
