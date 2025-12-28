package uz.qarzdorlar_ai.mapper;

import org.jvnet.hk2.annotations.Service;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.model.CurrencyDTO;

@Service
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
