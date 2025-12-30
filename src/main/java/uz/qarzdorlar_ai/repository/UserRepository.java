package uz.qarzdorlar_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.qarzdorlar_ai.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("""
                SELECT u
                FROM User u
                WHERE u.username = :username
                  AND u.role IN (
                      uz.qarzdorlar_ai.enums.Role.ADMIN,
                      uz.qarzdorlar_ai.enums.Role.DEVELOPER,
                      uz.qarzdorlar_ai.enums.Role.STAFF,
                      uz.qarzdorlar_ai.enums.Role.STAFF_PLUS
                  )
            """)
    Optional<User> findAllowedUserByUsername(@Param("username") String username);


    boolean existsByUsername(String username);
}