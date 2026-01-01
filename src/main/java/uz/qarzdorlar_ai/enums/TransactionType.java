package uz.qarzdorlar_ai.enums;

public enum TransactionType {

//    PURCHASE, // sotib olish
//    PURCHASE_PAYMENT, // purchasega To'lov
//    SALE,    // Sotuv
//    PAYMENT, // To'lov
//    RETURN_PAYMENT,
//    RETURN,  // Vozvrat
//    TRANSFER, // Mijozdan-mijozga o'tkazma (P2P)

    // ===== TOVAR AMALLARI =====
    PURCHASE,             // Taminotchidan tovar olish (Stock IN)
    SALE,                 // Mijozga tovar sotish (Stock OUT)
    RETURN_FROM_CLIENT,   // Mijozdan vozvrat (Stock IN)
    RETURN_TO_SUPPLIER,   // Taminotchiga vozvrat (Stock OUT)

    // ===== PUL AMALLARI (Moliya) =====
    PAYMENT_FROM_CLIENT,  // Mijozdan to'lov olish (Inflow)
    PAYMENT_TO_SUPPLIER,  // Taminotchiga to'lov qilish (Outflow)

    // QO'SHIMCHA (Zarur bo'lsa):
    PAYMENT_TO_CLIENT,    // Mijozga pulini qaytarish (Vozvrat puli)
    PAYMENT_FROM_SUPPLIER,// Taminotchi bizga pul qaytardi

    // ===== O'TKAZMA =====
    TRANSFER              // Client -> Client (P2P)

}