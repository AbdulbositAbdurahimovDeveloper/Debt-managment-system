package uz.qarzdorlar_ai.specification;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import uz.qarzdorlar_ai.model.Brand;
import uz.qarzdorlar_ai.model.Category;
import uz.qarzdorlar_ai.model.Product;
import uz.qarzdorlar_ai.payload.ProductFilterDTO;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> build(ProductFilterDTO criteria) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Umumiy "filter" (Global search across multiple columns)
            if (criteria.getFilter() != null && !criteria.getFilter().isBlank()) {
                String pattern = "%" + criteria.getFilter().toLowerCase() + "%";
                
                // Joinlar orqali brand va category nomidan ham qidirish
                Join<Product, Brand> brandJoin = root.join(Product.Fields.brand, JoinType.LEFT);
                Join<Product, Category> categoryJoin = root.join(Product.Fields.category, JoinType.LEFT);

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(Product.Fields.name)), pattern),
                        cb.like(cb.lower(root.get(Product.Fields.cpu)), pattern),
                        cb.like(cb.lower(root.get(Product.Fields.gpu)), pattern),
                        cb.like(cb.lower(root.get(Product.Fields.ram)), pattern),
                        cb.like(cb.lower(root.get(Product.Fields.storage)), pattern),
                        cb.like(cb.lower(root.get(Product.Fields.modelCode)), pattern),
                        cb.like(cb.lower(brandJoin.get(Brand.Fields.name)), pattern),
                        cb.like(cb.lower(categoryJoin.get(Category.Fields.name)), pattern)
                ));
            }

            // 2. Specific text fields
            addLikePredicate(predicates, cb, root.get(Product.Fields.name), criteria.getName());
            addLikePredicate(predicates, cb, root.get(Product.Fields.cpu), criteria.getCpu());
            addLikePredicate(predicates, cb, root.get(Product.Fields.gpu), criteria.getGpu());
            addLikePredicate(predicates, cb, root.get(Product.Fields.ram), criteria.getRam());
            addLikePredicate(predicates, cb, root.get(Product.Fields.os), criteria.getOs());
            addLikePredicate(predicates, cb, root.get(Product.Fields.color), criteria.getColor());
            addLikePredicate(predicates, cb, root.get(Product.Fields.modelCode), criteria.getModelCode());

            // 3. Relations (IDs)
            if (criteria.getBrandId() != null) {
                predicates.add(cb.equal(root.get(Product.Fields.brand).get("id"), criteria.getBrandId()));
            }
            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get(Product.Fields.category).get("id"), criteria.getCategoryId()));
            }

            // 4. Relations (Names)
            if (criteria.getBrandName() != null && !criteria.getBrandName().isBlank()) {
                Join<Product, Brand> brandJoin = root.join(Product.Fields.brand);
                predicates.add(cb.equal(cb.lower(brandJoin.get(Brand.Fields.name)), criteria.getBrandName().toLowerCase()));
            }

            // 5. Price Range
            if (criteria.getMinPriceUsd() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(Product.Fields.priceUsd), criteria.getMinPriceUsd()));
            }
            if (criteria.getMaxPriceUsd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(Product.Fields.priceUsd), criteria.getMaxPriceUsd()));
            }

            // 6. Booleans
            if (criteria.getTouchScreen() != null) {
                predicates.add(cb.equal(root.get(Product.Fields.touchScreen), criteria.getTouchScreen()));
            }
            if (criteria.getBacklit() != null) {
                predicates.add(cb.equal(root.get(Product.Fields.backlit), criteria.getBacklit()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addLikePredicate(List<Predicate> predicates, CriteriaBuilder cb, Expression<String> expression, String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(cb.like(cb.lower(expression), "%" + value.toLowerCase() + "%"));
        }
    }
}