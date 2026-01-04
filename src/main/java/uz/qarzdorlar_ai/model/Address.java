package uz.qarzdorlar_ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

@Getter
@Setter
@Entity
@Table(name = "addresses")
@FieldNameConstants
public class Address extends AbsLongEntity {

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String streetAndHouse;

    private Double latitude;

    private Double longitude;

    private String landmark;

    @OneToOne(optional = false)
    private User createdBy;

    @OneToOne(mappedBy = "clientAddress")
    private Client client;

}