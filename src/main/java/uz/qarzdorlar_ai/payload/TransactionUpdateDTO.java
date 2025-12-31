package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.TransactionStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for updating {@link uz.qarzdorlar_ai.model.Transaction}
 * 
 * Update qilish mumkin bo'lgan fieldlar:
 * - clientId: Mijozni o'zgartirish (balansni qayta hisoblash kerak)
 * - receiverClientId: TRANSFER uchun receiver client
 * - currencyId: Valyutani o'zgartirish (balansni qayta hisoblash kerak)
 * - exchangeRate: Kursni o'zgartirish (balansni qayta hisoblash kerak)
 * - clientExchangeRate: Client kursini o'zgartirish (balansni qayta hisoblash kerak)
 * - receiverExchangeRate: TRANSFER uchun receiver kursi
 * - originalAmount: PAYMENT, RETURN_PAYMENT, TRANSFER uchun summa
 * - feeAmount: Komissiya (balansni qayta hisoblash kerak)
 * - description: Tavsif
 * - items: SALE, RETURN, PURCHASE uchun mahsulotlar (balansni qayta hisoblash kerak)
 * - status: Tranzaksiya holati
 * 
 * O'zgartirish mumkin bo'lmagan fieldlar:
 * - type: Tranzaksiya turi (yangi transaction yaratish kerak)
 * - userId: Xodim (audit uchun)
 * - createdAt: Yaratilgan sana (audit uchun)
 * - usdAmount: Avtomatik hisoblanadi
 * - balanceAmount: Avtomatik hisoblanadi
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionUpdateDTO implements Serializable {

    private Long clientId;

    private Long receiverClientId;

    private Long currencyId;

    private BigDecimal exchangeRate;

    private BigDecimal clientExchangeRate;

    private BigDecimal receiverExchangeRate;

    private BigDecimal originalAmount;

    private BigDecimal feeAmount;

    private String description;

    private List<TransactionItemCreateDTO> items;

    private TransactionStatus status;

    private java.sql.Timestamp createdAt; // Migration uchun yoki sana o'zgartirish uchun

}

