package uz.qarzdorlar_ai.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductFilterDTO {

    private String filter;
    // Text search
    private String name;          // product nomi bo‘yicha
    private String cpu;
    private String gpu;
    private String ram;
    private String storage;
    private String os;
    private String color;
    private String modelCode;

    // Relations
    private Long brandId;
    private Long categoryId;

    // Brand & Category by NAME
    private String brandName;      // HP, Lenovo, Acer
    private String categoryName;   // Laptop, AIO, Printer

    // Price range (USD)
    private BigDecimal minPriceUsd;
    private BigDecimal maxPriceUsd;

    // Boolean filters
    private Boolean touchScreen;
    private Boolean backlit;
}
