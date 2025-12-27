package uz.qarzdorlar_ai.model;

import jakarta.persistence.*;
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
@SQLDelete(sql = "UPDATE exchange_rate SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class ExchangeRate extends AbsLongEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency currency;

    @Column(nullable = false)
    private BigDecimal rate; // 1 USD necha so'm ekanligi

}