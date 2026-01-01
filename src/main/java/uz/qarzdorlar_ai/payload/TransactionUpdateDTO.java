package uz.qarzdorlar_ai.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.CurrencyCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Transaction}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionUpdateDTO {

    // Quyidagi maydonlar o'zgarishi mumkin
    private BigDecimal amount;
    private BigDecimal marketRate;
    private BigDecimal clientRate;
    private BigDecimal receiverRate;
    private BigDecimal feeAmount;
    private CurrencyCode transactionCurrency;
    private String description;

    // Mahsulotlar ro'yxati (SALE/RETURN/PURCHASE uchun)
    private List<TransactionItemCreateDTO> items;
}

