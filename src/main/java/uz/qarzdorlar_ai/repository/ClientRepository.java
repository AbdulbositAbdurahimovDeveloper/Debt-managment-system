package uz.qarzdorlar_ai.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.qarzdorlar_ai.model.Client;

import java.math.BigDecimal;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByFullName(String fullName);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Pessimistic lock bilan client olish (race condition oldini olish uchun)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Client c WHERE c.id = :id AND c.deleted = false")
    Client findByIdWithLock(@Param("id") Long id);

    /**
     * SQL darajasida balansni yangilash (atomic operation)
     * Race condition oldini olish uchun
     */
    @Modifying
    @Query("UPDATE Client c SET c.currentBalance = c.currentBalance + :amount WHERE c.id = :id AND c.deleted = false")
    int updateBalanceAtomic(@Param("id") Long id, @Param("amount") BigDecimal amount);


//    @Modifying
//    @Query("UPDATE Client c SET c.currentBalance = COALESCE(c.currentBalance, 0) + :amount WHERE c.id = :clientId")
//    void updateBalance(@Param("clientId") Long clientId, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Client c SET c.currentBalance = COALESCE(c.currentBalance, 0) + :amount WHERE c.id = :clientId")
    void updateBalance(@Param("clientId") Long clientId, @Param("amount") BigDecimal amount);
}