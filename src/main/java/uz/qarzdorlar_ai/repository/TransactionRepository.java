package uz.qarzdorlar_ai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.qarzdorlar_ai.model.Transaction;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
                SELECT t
                FROM Transaction t
                WHERE t.client.id = :clientId
                   OR t.receiverClient.id = :clientId
            """)
    Page<Transaction> findAllByClientOrReceiver(
            @Param("clientId") Long clientId,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "JOIN FETCH t.client " +
            "LEFT JOIN FETCH t.receiverClient " +
            "WHERE t.id = :id")
    Optional<Transaction> findByIdWithDetails(@Param("id") Long id);


}