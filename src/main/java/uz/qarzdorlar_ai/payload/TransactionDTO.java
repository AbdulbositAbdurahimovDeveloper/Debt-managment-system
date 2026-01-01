package uz.qarzdorlar_ai.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.enums.TransactionStatus;
import uz.qarzdorlar_ai.enums.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Transaction}
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO implements Serializable {

    private Long id;
    private Long clientId;
    private Long receiverClientId;
    private Long userId; // createdBy

    private TransactionType type;
    private TransactionStatus status;

    // --- TRANZAKSIYA VALYUTASI ---
    private CurrencyCode transactionCurrency;
    private BigDecimal amount; // Amalda berilgan/olingan naqd pul
    private BigDecimal marketRate; // 1 USD = ? TransactionCurrency

    // --- USD (PIVOT) ---
    private BigDecimal usdAmount;
    private BigDecimal feeAmount; // USD da

    // --- MIJOZ BALANSI ---
    private CurrencyCode clientCurrency;
    private BigDecimal clientRate; // 1 USD = ? ClientCurrency
    private BigDecimal balanceEffect; // Balansga ta'sir qilgan yakuniy summa

    // --- RECEIVER (TRANSFER UCHUN) ---
    private BigDecimal receiverRate;

    private String description;
    private List<TransactionItemDTO> items;

    private boolean deleted = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}