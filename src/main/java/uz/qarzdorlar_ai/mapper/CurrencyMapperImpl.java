package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.payload.CurrencyDTO;

@Component
public class CurrencyMapperImpl implements CurrencyMapper {

    @Override
    public CurrencyDTO toDTO(Currency currency) {

        CurrencyDTO currencyDTO = new CurrencyDTO();
        currencyDTO.setId(currency.getId());
        currencyDTO.setName(currency.getName());
        currencyDTO.setCode(currency.getCode());
        currencyDTO.setSymbol(currency.getSymbol());
        currencyDTO.setBase(currency.isBase());
        currencyDTO.setDeleted(currencyDTO.isDeleted());
        currencyDTO.setCreatedAt(currency.getCreatedAt());
        currencyDTO.setUpdatedAt(currency.getUpdatedAt());

        return currencyDTO;
    }
}
