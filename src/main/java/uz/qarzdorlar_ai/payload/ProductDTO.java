package uz.qarzdorlar_ai.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.model.Product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO for {@link Product}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO implements Serializable {
    private Long id;
    private String name;
    private String brandName;
    private String categoryName;
    private BigDecimal price;
    private String cpu;
    private String ram;
    private String storage;
    private String gpu;
    private String display;
    private String resolution;
    private String os;
    private String color;
    private String modelCode;
    private Boolean touchScreen = false;
    private Boolean backlit = false;
    private String description;
    private String rawData;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}