package uz.qarzdorlar_ai.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link uz.qarzdorlar_ai.model.Product}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreateDTO implements Serializable {

    @NotNull
    @NotBlank
    private String name;

    private Long brandId;

    private Long categoryId;

    @PositiveOrZero
    private BigDecimal priceUsd;

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

    @NotNull
    @NotBlank
    private String rawData;
}