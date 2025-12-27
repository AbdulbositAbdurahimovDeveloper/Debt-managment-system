package uz.qarzdorlar_ai.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uz.qarzdorlar_ai.enums.UserState;

@Getter
@Setter
@Entity
@Table(name = "telegram_user")
public class TelegramUser {

    @Id
    private Long chatId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonBackReference
    @ToString.Exclude
    private User user;

    private UserState userState = UserState.NONE;
}
