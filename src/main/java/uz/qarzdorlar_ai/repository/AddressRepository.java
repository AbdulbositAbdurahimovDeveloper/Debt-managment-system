package uz.qarzdorlar_ai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Address;
import uz.qarzdorlar_ai.model.User;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByIdAndCreatedBy(Long id, User createdBy);

    Page<Address> findAllByCreatedBy(User createdBy, Pageable pageable);
}