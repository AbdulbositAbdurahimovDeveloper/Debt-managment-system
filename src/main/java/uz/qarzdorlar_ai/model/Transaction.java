package uz.qarzdorlar_ai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import uz.qarzdorlar_ai.enums.TransactionStatus;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@FieldNameConstants
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE transaction SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Transaction extends AbsLongEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // The person who pays or takes the money

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_client_id")
    private Client receiverClient; // Only for TRANSFER type (Ex: Courier gives money to Supplier)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Staff/Seller who performed this operation

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // SALE, PAYMENT, RETURN, TRANSFER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // PENDING, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = true)
    private Currency currency; // Currency used in this transaction (Ex: Paid in UZS)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal exchangeRate; // Rate: 1 USD = ? Transaction Currency (Ex: 12800)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal clientExchangeRate; // Rate: 1 USD = ? Client Balance Currency (Ex: 3.67 for AED)

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal originalAmount; // Exact amount given by client (Ex: 1,280,000 UZS)

    @Column(precision = 19, scale = 4)
    private BigDecimal receiverExchangeRate; // Receiver's rate (Ex: 3.67 for AED)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal usdAmount; // Calculated value in USD (Ex: 100 USD) // 2000$

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAmount; // Final amount to affect Client's Ledger (Ex: 100 USD or 367 AED)

    @Column(precision = 19, scale = 4)
    private BigDecimal feeAmount; // Courier's commission or service fee (Ex: 50 USD)

    @Column(columnDefinition = "TEXT")
    private String description; // Notes for transaction (Ex: "Transfer to Supplier through courier X")

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionItem> items; // Product details (Only for SALE and RETURN)
}