package uz.qarzdorlar_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Currency;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {
}