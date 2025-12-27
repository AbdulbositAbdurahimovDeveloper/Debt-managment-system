package uz.qarzdorlar_ai.eventListener;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.payload.ProductInsertEvent;
import uz.qarzdorlar_ai.payload.ProductParseDTO;
import uz.qarzdorlar_ai.repository.BrandRepository;
import uz.qarzdorlar_ai.repository.CategoryRepository;
import uz.qarzdorlar_ai.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductsInsertListener {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Async
    @EventListener
    @Transactional
    public void handleProductInserted(ProductInsertEvent event) { // Wrapper klass ishlatildi
        List<ProductParseDTO> productParseDTOS = event.products();
        log.info("Received {} products to save into database", productParseDTOS.size());

        for (ProductParseDTO dto : productParseDTOS) {
            try {
                // 1. Category logic
                String categoryName = dto.getCategoryName(); // DTO dagi field nomi bilan bir xil bo'lsin
                Category category = categoryRepository.findByName(categoryName)
                        .orElseGet(() -> categoryRepository.save(new Category(categoryName)));

                // 2. Brand logic
                String brandName = dto.getBrandName();
                Brand brand = brandRepository.findByName(brandName)
                        .orElseGet(() -> brandRepository.save(new Brand(brandName)));

                // 3. Product mapping
                Product product = new Product();
                product.setName(dto.getName());
                product.setBrand(brand);
                product.setCategory(category);

                // Price parsing (AI double berishi mumkin, shuni tekshiring)
                product.setPriceUsd(parsePrice(dto.getPrice()));

                product.setCpu(dto.getCpu());
                product.setRam(dto.getRam());
                product.setStorage(dto.getStorage());
                product.setGpu(dto.getGpu());
                product.setDisplay(dto.getDisplay());
                product.setResolution(dto.getResolution());
                product.setOs(dto.getOs());
                product.setColor(dto.getColor());
                product.setModelCode(dto.getModelCode());
                product.setTouchScreen(dto.getTouchScreen());
                product.setBacklit(dto.getBacklit());

                product.setRawData(dto.getRawData().trim()); // Yoki original text

                productRepository.save(product);
            } catch (Exception e) {
                log.error("Error saving product: {}", dto.getName(), e);
            }
        }
        log.info("Batch save completed.");
    }

    public BigDecimal parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return BigDecimal.ZERO;
        // Bo'shliqlarni olib tashlash va vergulni nuqtaga almashtirish
        String cleaned = priceStr.replace(" ", "").replace(",", ".");
        return new BigDecimal(cleaned);
    }
}
