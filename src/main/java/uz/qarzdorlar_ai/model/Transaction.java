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
@SQLDelete(sql = "UPDATE transaction SET deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Transaction extends AbsLongEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client; // bu doim boldi kimgadur yoki kimdandur

    @ManyToOne(fetch = FetchType.LAZY)
    private Client receiverClient; // bu transfer holatida boladi faqat

    @Enumerated(EnumType.STRING)
    private TransactionType type; // bu doim shart chunki type bolmasa tr ham bolmaydi

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.COMLATED; // bu tr holati

    // 1. TRANZAKSIYA VALYUTASI
    @Enumerated(EnumType.STRING)
    private CurrencyCode transactionCurrency;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount; // Tranzaksiya amalga oshgan summasi (Masalan: 100 AED)

    // 2. KURS SNAPSHOTLARI (Juniorlar adashmasligi uchun eng muhimi)
    @Column(precision = 19, scale = 4)
    private BigDecimal rateToUsd; // 1 USD necha transactionCurrency ekanligi (Masalan: 3.67)

    @Column(precision = 19, scale = 4)
    private BigDecimal usdAmount; // amount / rateToUsd (Tizim asosi - 27.24 USD)

    // 3. MIJOZLAR BALANSIGA TA'SIR
    // Yuboruvchi (Client) uchun
    @Column(precision = 19, scale = 4)
    private BigDecimal clientRateSnapshot; // Clientning valyutasi kursi ($ ga nisbatan)

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceEffect; // Client balansiga necha pul bo'lib boradi (+/-)

    // Qabul qiluvchi (Receiver) uchun - FAQAT TRANSFERda ishlatiladi
    @Column(precision = 19, scale = 4)
    private BigDecimal receiverRateSnapshot; // Receiverning valyutasi kursi

    @Column(precision = 19, scale = 4)
    private BigDecimal receiverBalanceEffect; // Receiver balansiga necha pul bo'lib boradi (+)

    // 4. XARAJAT (Fee)
    @Column(precision = 19, scale = 4)
    private BigDecimal feeAmount; // Bu USD da bo'lishi qulay (CASH_OUT yoki TRANSFER uchun)

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionItem> items;

}