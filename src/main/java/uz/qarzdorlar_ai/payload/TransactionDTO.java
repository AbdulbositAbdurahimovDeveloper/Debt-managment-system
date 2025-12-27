package uz.qarzdorlar_ai.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDTO {
    private String data;    // YYYY-MM-DD
    private String user;    // Ism yoki null
    private String type;    // BUY, PAY, RETURN
    private String item;    // Tovar nomi
    private Integer count;  // Soni
    private BigDecimal price; // Narxi
    private BigDecimal tolov; // To'langan pul
    private String valyuta;   // USD, UZS
    private String comment;   // Izoh
}