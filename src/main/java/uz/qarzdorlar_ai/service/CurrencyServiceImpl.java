package uz.qarzdorlar_ai.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.exception.DataConflictException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.CurrencyMapper;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.payload.CurrencyCreateDTO;
import uz.qarzdorlar_ai.payload.CurrencyDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.repository.CurrencyRepository;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyMapper currencyMapper;
    private final CurrencyRepository currencyRepository;


    @Override
    @Transactional
    public CurrencyDTO createCurrency(CurrencyCreateDTO dto) {

        Currency currency = new Currency();

        if (currencyRepository.existsByCode(dto.getCode())) {
            throw new DataConflictException("Currency already exist with code : " + dto.getCode());
        }

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
    public CurrencyDTO updateCurrency(Long id, CurrencyCreateDTO dto) {

        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id : " + id)
                );

        if (dto.getName() != null) {

            if (Objects.equals(dto.getName(), currency.getName())
                    & currencyRepository.existsByName(dto.getName())) {
                currency.setName(dto.getName());
            }
        }

        if (dto.getCode() != null) {
            currency.setCode(dto.getCode());
        }

        if (dto.getSymbol() != null) {
            currency.setSymbol(dto.getSymbol());
        }

        currency.setBase(dto.isBase());

        currencyRepository.save(currency);

        return currencyMapper.toDTO(currency);
    }

    @Override
    public String deleteCurrency(Long id) {

        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id : " + id)
                );

        currencyRepository.delete(currency);

        return "Currency deleted successfully with id : " + currency.getId();
    }
}
