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

    // 1. KIM ISHTIROK ETYAPTI?
    @NotNull
    private Long clientId;           // ASOSIY MIJOZ: Sotuvda xaridor, To'lovda pul beruvchi.

    private Long receiverClientId;   // QABUL QILUVCHI: Faqat TRANSFER (O'tkazma)da kerak.
                                     // Boshqa hollarda null bo'ladi.

    // 2. QANDAY HARAKAT?
    @NotNull
    private TransactionType type;    // SALE, CASH_IN, TRANSFER va hokazo.

    // 3. TO'LOV MA'LUMOTLARI (Transaction Level)
    @NotNull
    private CurrencyCode transactionCurrency; // TO'LOV VALYUTASI: Mijoz kassaga nima berdi? (UZS, USD yoki AED)

    private BigDecimal amount;  // TO'LOV SUMMASI: Mijoz bergan pul miqdori (O'sha valyutada).
                                // Agarda SALE bo'lsa, bu yerga itemlarning umumiy summasini yozsa ham bo'ladi.

    private BigDecimal rateToUsd;    // TO'LOV KURSI: 1 USD necha [transactionCurrency] bo'ladi?
                                     // Masalan: 12800 (UZS bo'lsa) yoki 3.67 (AED bo'lsa).
                                     // Bu amount-ni USD-ga (Oltin ko'prikka) o'tkazish uchun kerak.

    // 4. MIJOZ BALANSI UCHUN (Client Balance Level)
    private BigDecimal clientRateToUsd; // MIJOZ KURSI: 1 USD necha [Client.mainCurrency] bo'ladi?
                                        // Juniorga: "Mijozning o'z valyutasidagi kursini Frontenddan olib ber!" deyiladi.
                                        // Bu USD-dan Mijoz balansiga (UZS/AED) o'tkazish uchun kerak.

    // 5. QABUL QILUVCHI BALANSI UCHUN (Receiver Level - FAQAT TRANSFER)
    private BigDecimal receiverRateToUsd; // QABUL QILUVCHI KURSI: 1 USD necha [ReceiverClient.mainCurrency] bo'ladi?
                                          // Faqat Transferda ishlatiladi.

    // 6. QO'SHIMCHA
    private BigDecimal feeAmount;    // XIZMAT HAQI: Masalan, TRANSFER yoki CASH_OUT bo'lsa,
                                    // kuryer yoki bank oladigan foiz/summa (USDda yuborish tavsiya qilinadi).

    private String description;      // IZOH: Tranzaksiya haqida qisqacha ma'lumot.

    // 7. MAHSULOTLAR (Agar SALE/PURCHASE/RETURN bo'lsa)
    private List<TransactionItemCreateDTO> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Timestamp createdAt;

}