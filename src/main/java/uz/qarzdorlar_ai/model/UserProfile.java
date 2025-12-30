package uz.qarzdorlar_ai.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

@Getter
@Setter
@Entity(name = "user_profiles")
@SQLDelete(sql = "UPDATE user_profiles SET deleted = true WHERE id = ?")
@SQLRestriction(value = "deleted=false")
@FieldNameConstants
public class UserProfile extends AbsLongEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    private String phoneNumber;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonBackReference
    private User user;

    private boolean emailEnabled = false;
}