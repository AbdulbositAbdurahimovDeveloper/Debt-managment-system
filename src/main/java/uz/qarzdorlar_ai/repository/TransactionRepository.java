package uz.qarzdorlar_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}