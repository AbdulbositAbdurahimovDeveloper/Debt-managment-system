package uz.qarzdorlar_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Currency;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    boolean existsByName(String name);

    List<Currency> findByCode(String code);

    boolean existsByCode(String code);
}