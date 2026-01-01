package uz.qarzdorlar_ai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.enums.TransactionStatus;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldNameConstants
@Entity
@Table(indexes = {
        @Index(name = "idx_transection_clinets", columnList = "Client_id"),
        @Index(name = "idx_transection_staff_users", columnList = "created_by_id")
})
@NoArgsConstructor
@AllArgsConstructor
//@SQLDelete(sql = "UPDATE transaction SET deleted = true WHERE id = ?")
@SQLDelete(sql = "UPDATE transactions SET deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Transaction extends AbsLongEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client; // Asosiy mijoz

    @ManyToOne(fetch = FetchType.LAZY)
    private Client receiverClient; // TRANSFER turi uchun (masalan, kuryer pulni topshirgan supplier)

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.COMLATED;

    // --- TRANZAKSIYA VALYUTASI (To'lov paytidagi amaldagi summa) ---
    @Enumerated(EnumType.STRING)
    private CurrencyCode transactionCurrency;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount; // Amalda berilgan summa (Masalan: 12,800,000 UZS)

    @Column(precision = 19, scale = 4)
    private BigDecimal marketRate; // Kurs: 1 USD = 12800 UZS tr paytidagi valyuta dolorga nisbati

    // --- USD (Tizimning ichki o'lchov birligi) ---
    @Column(precision = 19, scale = 4)
    private BigDecimal usdAmount; // (12,800,000 / 12800 = 1000 USD)

    // --- XIZMAT HAQI (Fee) ---
    @Column(precision = 19, scale = 4)
    private BigDecimal feeAmount; // Kuryer haqi yoki xizmat haqi (USD da saqlash tavsiya etiladi)

    // --- MIJOZ BALANSIGA TA'SIR ---
    @Enumerated(EnumType.STRING)
    private CurrencyCode clientCurrency; // Mijozning balans valyutasi (Snapshot)

    @Column(precision = 19, scale = 4)
    private BigDecimal clientRate; // Kurs: 1 USD = ? ClientCurrency (Masalan: 3.67 AED)

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceEffect; // Balansga ta'sir qiluvchi yakuniy summa (Mijoz valyutasida)

    // --- TRANSFER UCHUN RECEIVER BALANSI ---
    @Column(precision = 19, scale = 4)
    private BigDecimal receiverRate; // Kurs: 1 USD = ? ReceiverCurrency

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionItem> items;

    public void updateItems(List<TransactionItem> newItems) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.clear(); // Eskilarini o'chirish (Hibernate buni kuzatadi)
        if (newItems != null) {
            for (TransactionItem item : newItems) {
                item.setTransaction(this);
                this.items.add(item); // Mavjud ro'yxatga qo'shish
            }
        }
    }

}
//
//package uz.qarzdorlar_ai.model;
//
//import jakarta.persistence.*;
//        import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.SQLDelete;
//import org.hibernate.annotations.SQLRestriction;
//import uz.qarzdorlar_ai.enums.CurrencyCode;
//import uz.qarzdorlar_ai.enums.TransactionType;
//import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
//@Getter
//@Setter
//@Entity
//@Table(name = "transactions", indexes = {
//        @Index(name = "idx_transaction_client", columnList = "client_id"),
//        @Index(name = "idx_transaction_courier", columnList = "courier_id"),
//        @Index(name = "idx_transaction_type", columnList = "transaction_type")
//})
//@NoArgsConstructor
//@AllArgsConstructor
//@SQLDelete(sql = "UPDATE transactions SET deleted = true WHERE id = ?")
//@SQLRestriction("deleted = false")
//public class Transaction extends AbsLongEntity {
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private TransactionType transactionType;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "client_id", nullable = false)
//    private Client client; // Asosiy mijoz (Xaridor, Taminotchi yoki Kuryer)
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "target_client_id")
//    private Client targetClient; // Faqat TRANSFER (P2P) holatida pulni oluvchi
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "courier_id")
//    private Client courier; // Pulni tashiydigan vositachi (Kuryer)
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private CurrencyCode transactionCurrency; // Tranzaksiya qaysi valyutada bo'ldi (USD, UZS, AED)
//
//    @Column(precision = 19, scale = 4, nullable = false)
//    private BigDecimal amount; // Tranzaksiyaning haqiqiy summasi (o'z valyutasida)
//
//    @Column(precision = 19, scale = 4, nullable = false)
//    private BigDecimal exchangeRate; // O'sha vaqtdagi kurs (Mijozning asosiy valyutasiga nisbatan)
//
//    @Column(precision = 19, scale = 4, nullable = false)
//    private BigDecimal convertedAmount; // Mijoz balansiga ta'sir qiluvchi summa (Base currencyda)
//
//    @Column(precision = 19, scale = 4)
//    private BigDecimal courierFee; // Kuryerning xizmat haqi (agar kuryer bo'lsa)
//
//    private String description; // Izoh
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private User createdBy; // Tranzaksiyani kiritgan xodim
//
//    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<TransactionItem> items = new ArrayList<>();
//
//    // Helper method to add items
//    public void addItem(TransactionItem item) {
//        items.add(item);
//        item.setTransaction(this);
//    }
//}