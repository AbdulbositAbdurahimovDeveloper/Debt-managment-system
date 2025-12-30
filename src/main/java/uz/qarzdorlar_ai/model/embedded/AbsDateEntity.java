package uz.qarzdorlar_ai.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Getter
@Setter
@MappedSuperclass
@FieldNameConstants
public abstract class AbsDateEntity {

    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) { // Agar qo'lda berilmagan bo'lsa
            this.createdAt = new Timestamp(System.currentTimeMillis());
        }
    }

}