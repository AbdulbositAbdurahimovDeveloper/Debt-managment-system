package uz.qarzdorlar_ai.repository;

import org.springframework.data.geo.GeoResult;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {


    Optional<Product> findByRawData(String rawData);

    Optional<Product> findByRawDataStartingWith(String rawData);

    Optional<Product> findByRawDataStartsWithIgnoreCase(String rawData);

    Optional<Product> findByRawDataLike(String rawData);

    Optional<Product> findByRawDataStartsWith(String rawData);

    boolean existsByRawDataStartingWith(String rawData);

    boolean existsByRawData(String rawData);
}