package uz.qarzdorlar_ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.model.UserProfile;
import uz.qarzdorlar_ai.repository.UserRepository;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Starting system user initialization...");

        createIfNotExists("develop", "Backend", "Developer", Role.DEVELOPER);
        createIfNotExists("admin", "Main", "Admin", Role.ADMIN);
        createIfNotExists("staff", "Site", "Staff", Role.STAFF);
        createIfNotExists("staff_plus", "Staff", "Plus", Role.STAFF_PLUS);
        createIfNotExists("user", "Default", "User", Role.USER);
        createIfNotExists("client", "Default", "Client", Role.CLIENT);
        createIfNotExists("anonymous", "Anonymous", "User", Role.ANONYMOUS);

        log.info("System user initialization finished.");
    }


    /**
     * Creates a system user only if it does not already exist.
     * Safe for multiple application restarts.
     */
    private void createIfNotExists(
            String username,
            String firstName,
            String lastName,
            Role role
    ) {
        try {
            if (userRepository.existsByUsername(username)) {
                log.info("User '{}' already exists. Skipping.", username);
                return;
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("1234"));
            user.setRole(role);

            UserProfile profile = new UserProfile();
            profile.setFirstName(firstName);
            profile.setLastName(lastName);

            // 🔴 MUHIM: relationship
            profile.setUser(user);
            user.setUserProfile(profile);

            userRepository.save(user);

            log.info(
                    "User '{}' created successfully (role={}, name={} {})",
                    username, role, firstName, lastName
            );

        } catch (Exception e) {
            log.error(
                    "FAILED to create user '{}'. Reason: {}",
                    username, e.getMessage()
            );
        }
    }
}
