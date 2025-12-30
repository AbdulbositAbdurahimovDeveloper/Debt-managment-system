package uz.qarzdorlar_ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

import java.math.BigDecimal;

@Getter
@Setter
@FieldNameConstants
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE transaction_item SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class TransactionItem extends AbsLongEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private Integer quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal unitPrice; // Product price in USD at the time of sale

    @Column(precision = 19, scale = 4)
    private BigDecimal totalPrice; // quantity * unitPriceUsd
}