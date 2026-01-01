package uz.qarzdorlar_ai.payload.sheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkJournalDTO {

    private String id;
    private String date;
    private String type; // [BUY,CASH-OUT,PAY,RETURN]
    private String courier;
    private String supplier;
    private String item;
    private String count;
    private String price;
    private String debtAed;
    private String paidAed;
    private String cashUsd;
    private String rate;
    private String fee;
    private String usedUsd;
    private String comment;
    private String productId;

}
