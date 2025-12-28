package uz.qarzdorlar_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.qarzdorlar_ai.model.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {


    boolean existsByFullName(String fullName);

    boolean existsByPhoneNumber(String phoneNumber);
}