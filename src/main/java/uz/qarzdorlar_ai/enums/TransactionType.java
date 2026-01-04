package uz.qarzdorlar_ai.enums;

public enum TransactionType {

    SALE,               // Sotish (Ombordan tovar chiqdi)
    PURCHASE,           // Sotib olish (Omborga tovar kirdi)
    RETURN,             // Tovar qaytarish (Mijozdan bizga qaytib kirdi)
    // Pul harakati
    CASH_IN,            // Pul olish (Kassaga pul kirdi - kimdan bo'lishidan qat'iy nazar)
    CASH_OUT,           // Pul berish (Kassadan pul chiqdi - kimga bo'lishidan qat'iy nazar)
    // O'tkazma
    TRANSFER

}