package uz.qarzdorlar_ai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import uz.qarzdorlar_ai.model.embedded.AbsLongEntity;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Getter
@Setter
@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_brand", columnList = "brand_id"),
        @Index(name = "idx_product_category", columnList = "category_id")
})
@SQLDelete(sql = "UPDATE product SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Product extends AbsLongEntity {

    @Column(nullable = false)
    private String name; // To'liq nomi: "HP OmniStudio X 32"

    @ManyToOne(fetch = FetchType.LAZY)
    private Brand brand; // HP, Lenovo, Microsoft, Acer...

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category; // Laptop, AIO, Printer, Mouse, etc.

    @Column(precision = 19, scale = 4)
    private BigDecimal priceUsd; // Base price in USD (Always $)

    private String cpu; // Intel Core Ultra7 155H, i7-13700T

    private String ram; // 32GB DDR5, 16GB DDR4

    private String storage; // 1TB SSD, 512GB SSD M.2, 1TB HDD

    private String gpu; // RTX 4050 6GB, Intel Iris X, MX 350

    private String display; // 31.5" 4K UHD IPS, 14" OLED

    private String resolution; // 4K, FHD, 2.8K, WUXGA

    private String os; // Windows 11 Home, Free DOS

    private String color; // Silver, Black, Snowflake White

    private String modelCode; // (979L2UA), (6J6L2AV) - qavs ichidagi kodlar

    @Column(name = "is_touchscreen")
    private Boolean touchScreen = false;

    @Column(name = "is_backlit")
    private Boolean backlit = false;

    @Column(columnDefinition = "TEXT")
    private String description; // Agar qo'shimcha info bo'lsa (masalan: "Ekranida dog'i bor")

    @Column(name = "raw_data",nullable = false, columnDefinition = "TEXT")
    private String rawData; // Audit data from Google Sheets
}