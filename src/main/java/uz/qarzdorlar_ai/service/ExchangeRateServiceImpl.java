package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.ExchangeRateMapper;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.model.ExchangeRate;
import uz.qarzdorlar_ai.payload.ExchangeRateDTO;
import uz.qarzdorlar_ai.model.embedded.AbsDateEntity;
import uz.qarzdorlar_ai.payload.ExchangeRateCreateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.repository.CurrencyRepository;
import uz.qarzdorlar_ai.repository.ExchangeRateRepository;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateMapper exchangeRateMapper;
    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public ExchangeRateDTO createExchangeRate(ExchangeRateCreateDTO dto) {

        Currency currency = currencyRepository.findById(dto.getCurrencyId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id : " + dto.getCurrencyId())
                );

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCurrency(currency);
        exchangeRate.setRate(dto.getRate());

        exchangeRateRepository.save(exchangeRate);

        return exchangeRateMapper.toDTO(exchangeRate);
    }

    @Override
    public ExchangeRateDTO getByIdExchangeRate(Long id) {

        ExchangeRate exchangeRate = exchangeRateRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("ExchangeRate not found with id : " + id)
                );

        return exchangeRateMapper.toDTO(exchangeRate);

    }

    @Override
    public PageDTO<ExchangeRateDTO> getAllExchangeRate(Integer page, Integer size) {

        Sort sort = Sort.by(AbsDateEntity.Fields.createdAt);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<ExchangeRate> exchangeRates = exchangeRateRepository.findAll(pageRequest);

        return new PageDTO<>(
                exchangeRates.getContent().stream().map(exchangeRateMapper::toDTO).toList(),
                exchangeRates
        );
    }

    @Override
    public ExchangeRateDTO updateExchangeRate(Long id, ExchangeRateCreateDTO dto) {

        ExchangeRate exchangeRate = exchangeRateRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("ExchangeRate not found with id : " + id)
                );


        if (dto.getCurrencyId() != null) {
            Currency currency = currencyRepository.findById(dto.getCurrencyId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("Currency not found with id : " + dto.getCurrencyId())
                    );

            exchangeRate.setCurrency(currency);
        }
        exchangeRate.setRate(dto.getRate());

        return exchangeRateMapper.toDTO(exchangeRate);
    }

    @Override
    public String deleteExchangeRate(Long id) {

        ExchangeRate exchangeRate = exchangeRateRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("ExchangeRate not found with id : " + id)
                );

        exchangeRateRepository.delete(exchangeRate);

        return "ExchangeRate deleted successfully.Deleted exchange rate with id : " + id;
    }
}
