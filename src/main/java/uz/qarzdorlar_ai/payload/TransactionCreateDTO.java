package uz.qarzdorlar_ai.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.model.Client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Transaction}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCreateDTO implements Serializable {

    @NotNull
    private Long clientId;

    private Long receiverClientId;

    @NotNull
    private TransactionType type;

    @NotNull
    private CurrencyCode transactionCurrency;

    private BigDecimal amount;

    @NotNull
    @PositiveOrZero
    private BigDecimal marketRate;

    @PositiveOrZero
    private BigDecimal feeAmount;

    private BigDecimal clientRate;

    private BigDecimal receiverRate;

    private String description;

    private List<TransactionItemCreateDTO> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Timestamp createdAt;


//    @NotNull(message = "Mijoz tanlanishi shart")
//    private Long clientId; // Asosiy mijoz (Sotib oluvchi, To'lovchi, Jo'natuvchi)
//
//    @NotNull(message = "Tranzaksiya turi shart")
//    private TransactionType type;
//
//    // --- PUL MIQDORI VA VALYUTASI ---
//    @NotNull(message = "Valyuta kodi shart")
//    private CurrencyCode transactionCurrency; // Qaysi valyutada pul berildi/olindi? (UZS, USD, AED)
//
////    @NotNull(message = "Summa bo'sh bo'lishi mumkin emas")
//    private BigDecimal amount; // Amaldagi summa (Masalan: 1000 dirham yoki 12 mln so'm)
//
//    // --- RATE (KURSLAR) ---
//    // 1 USD necha transactionCurrency bo'lishi (Masalan: 12800)
//    @NotNull(message = "Bozor kursi shart")
//    private BigDecimal marketRate;
//
//    // 1 USD necha ClientCurrency bo'lishi (Masalan: 3.67)
//    // Mijozning balans valyutasiga o'tkazish uchun kerak
//    @NotNull(message = "Mijoz kursi shart")
//    private BigDecimal clientRate;
//
//    // --- TRANSFER (P2P) UCHUN ---
//    private Long receiverClientId; // Faqat TRANSFER bo'lganda to'ldiriladi
//    private BigDecimal receiverRate; // 1 USD necha ReceiverCurrency bo'lishi
//
//    // --- TOVARLAR (SALE, PURCHASE, RETURN uchun) ---
//    private List<TransactionItemCreateDTO> items; // Agar to'lov bo'lsa, bu empty bo'ladi
//
//    private String description;
//
//    private Timestamp createdAt;
//
//    private BigDecimal feeAmount;
}