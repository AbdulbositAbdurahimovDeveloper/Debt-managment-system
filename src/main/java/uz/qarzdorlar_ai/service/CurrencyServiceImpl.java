package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.CurrencyMapper;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.model.CurrencyCreateDTO;
import uz.qarzdorlar_ai.model.CurrencyDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.repository.CurrencyRepository;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyMapper currencyMapper;
    private final CurrencyRepository currencyRepository;


    @Override
    public CurrencyDTO createCurrency(CurrencyCreateDTO dto) {

        Currency currency = new Currency();
        currency.setName(dto.getName());
        currency.setCode(dto.getCode());
        currency.setSymbol(dto.getSymbol());
        currency.setBase(dto.isBase());

        currencyRepository.save(currency);

        return currencyMapper.toDTO(currency);
    }

    @Override
    public CurrencyDTO getByIdCurrency(Long id) {

        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id : " + id)
                );

        return currencyMapper.toDTO(currency);
    }

    @Override
    public PageDTO<CurrencyDTO> getAllCurrency(Integer page, Integer size) {

        Sort sort = Sort.by(Currency.Fields.name);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Currency> currencyPage = currencyRepository.findAll(pageRequest);

        return new PageDTO<>(
                currencyPage.getContent().stream().map(currencyMapper::toDTO).toList(),
                currencyPage
        );
    }

    @Override
    public CurrencyDTO updateCurrency(Long id, CurrencyCreateDTO currencyUpdateDTO) {
        return null;
    }

    @Override
    public String deleteCurrency(Long id) {
        return "";
    }
}
