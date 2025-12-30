package uz.qarzdorlar_ai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username")
})
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@SQLRestriction(value = "deleted=false")
@FieldNameConstants
public class User extends AbsLongEntity implements UserDetails {

    @Column(nullable = false,unique = true)
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private TelegramUser telegramUser;

    @OneToOne(mappedBy = "user", orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonManagedReference
    private UserProfile userProfile;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}