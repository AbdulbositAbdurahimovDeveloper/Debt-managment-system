package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.ExchangeRate;
import uz.qarzdorlar_ai.payload.ExchangeRateDTO;

@Component
public class ExchangeRateMapperImpl implements ExchangeRateMapper{

    @Override
    public ExchangeRateDTO toDTO(ExchangeRate exchangeRate) {
        return new ExchangeRateDTO(
                exchangeRate.getId(),
                exchangeRate.getCurrency().getId(),
                exchangeRate.getRate(),
                exchangeRate.isDeleted(),
                exchangeRate.getCreatedAt(),
                exchangeRate.getUpdatedAt()
        );
    }
}
