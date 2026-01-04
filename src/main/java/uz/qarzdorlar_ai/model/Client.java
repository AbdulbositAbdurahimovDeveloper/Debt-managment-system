package uz.qarzdorlar_ai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import uz.qarzdorlar_ai.enums.ClientType;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

import java.math.BigDecimal;

@Getter
@Setter
@FieldNameConstants
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE client SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Client extends AbsLongEntity {

    @Column(nullable = false, unique = true)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType type; // CLIENT, SUPPLIER, COURIER

    @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode = CurrencyCode.USD;

    @Column(precision = 19, scale = 4)
    private BigDecimal initialBalance; // Initial debt/credit from Google Sheets

    private BigDecimal currentBalance; //

    private String address;
    private String comment;

    @OneToOne(fetch = FetchType.LAZY)
    private TelegramUser telegramUser;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user; // Linked user for system login

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "addresses_od")
    private Address clientAddress;
}