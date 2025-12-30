package uz.qarzdorlar_ai.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.enums.ClientType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Client}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientDTO implements Serializable {

    private Long id;
    private Long userId;
    private String address;
    private String comment;
    private ClientType type;
    private String fullName;
    private String phoneNumber;
    private Long balanceCurrencyId;
    private Long telegramUserChatId;
    private BigDecimal currentBalance;
    private boolean deleted = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;

}