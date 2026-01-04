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
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO {

    // 1. ASOSIY MA'LUMOTLAR (Metadata)
    private Long id;                  // Tranzaksiya IDsi
    private Timestamp createdAt;  // Tranzaksiya amalga oshgan vaqt
    private TransactionType type;     // SALE, TRANSFER, CASH_IN va h.k.
    private TransactionStatus status; // COMPLETED, PENDING, CANCELED
    private String description;       // Tranzaksiya izohi

    // 2. ISHTIROKCHILAR (Participants)
    private Long clientId;            // Asosiy mijoz IDsi
    private String clientFullName;    // Mijoz ismi (Frontendda ko'rinishi uchun qulay)

    private Long receiverId;          // Qabul qiluvchi IDsi (Faqat Transferda)
    private String receiverFullName;  // Qabul qiluvchi ismi

    // 3. TO'LOV TAFSILOTLARI (Transaction Currency)
    // Mijoz amalda nima berganini ko'rsatadi
    private CurrencyCode transactionCurrency; // Amalda to'langan valyuta (Masalan: AED)
    private BigDecimal amount;               // Amalda to'langan summa (Masalan: 367 AED)
    private BigDecimal rateToUsd;            // To'lov vaqtidagi 1 USD = ? AED kursi

    // 4. STANDARTLASHTIRILGAN QIYMAT (Pivot Currency)
    // Juniorga: "Bu hisobotlar uchun eng muhim maydon"
    private BigDecimal usdAmount;            // Tranzaksiyaning dollardagi qiymati ($100)

    // 5. MIJOZ BALANSIGA TA'SIRI (Mijozning o'z valyutasida)
    // Mijoz o'zining "Mening qarzim qancha bo'ldi?" degan savoliga javobni shu yerdan oladi
    private CurrencyCode clientMainCurrency; // Mijozning balans valyutasi (Masalan: UZS)
    private BigDecimal balanceEffect;        // Uning balansiga necha so'm bo'lib ta'sir qildi (+/-)
    private BigDecimal clientRateSnapshot;   // O'sha paytdagi 1 USD = ? UZS kursi

    // 6. QABUL QILUVCHIGA TA'SIRI (Faqat Transferda)
    private CurrencyCode receiverMainCurrency; // Qabul qiluvchining balans valyutasi
    private BigDecimal receiverBalanceEffect;  // Qabul qiluvchi balansiga ta'siri
    private BigDecimal receiverRateSnapshot;   // Qabul qiluvchi kursi

    // 7. QO'SHIMCHA
    private BigDecimal feeAmount;              // Olingan xizmat haqi (USDda)
    private String createdByName;              // Tranzaksiyani qaysi xodim kiritdi?

    // 8. MAHSULOTLAR RO'YXATI (Agar SALE bo'lsa)
    private List<TransactionItemDTO> items;
}