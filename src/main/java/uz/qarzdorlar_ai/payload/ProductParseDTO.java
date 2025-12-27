package uz.qarzdorlar_ai.payload;

import lombok.Data;
import uz.qarzdorlar_ai.model.Product;

import java.math.BigDecimal;

/**
 * DTO for {@link Product}
 */
@Data
public class ProductParseDTO {
    private String name;
    private String brandName;
    private String categoryName;
    private String price;
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
}