package uz.qarzdorlar_ai.repository;

import com.google.common.io.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.model.ExchangeRate;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findFirstByCurrencyIdOrderByCreatedAtDesc(Long currencyId);
    Optional<ExchangeRate> findFirstByCurrencyOrderByCreatedAtDesc(Currency currency);
}