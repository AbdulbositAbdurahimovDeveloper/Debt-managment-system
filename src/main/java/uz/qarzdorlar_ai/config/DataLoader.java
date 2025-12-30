package uz.qarzdorlar_ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.exception.DataConflictException;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.model.UserProfile;
import uz.qarzdorlar_ai.payload.CurrencyCreateDTO;
import uz.qarzdorlar_ai.payload.CurrencyDTO;
import uz.qarzdorlar_ai.payload.ExchangeRateCreateDTO;
import uz.qarzdorlar_ai.repository.CurrencyRepository;
import uz.qarzdorlar_ai.repository.UserRepository;
import uz.qarzdorlar_ai.service.CurrencyService;
import uz.qarzdorlar_ai.service.ExchangeRateService;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final CurrencyRepository currencyRepository;

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

        createIfNotExistsCurrency("Uzbekistan Som", "UZS", "so'm", false, BigDecimal.valueOf(12100));
        createIfNotExistsCurrency("US Dollar", "USD", "$", true, BigDecimal.valueOf(1));
        createIfNotExistsCurrency("United Arab Emirates Dirham", "AED", "د.إ", false, BigDecimal.valueOf(3.67));

        log.info("System user initialization finished.");
    }


    private void createIfNotExistsCurrency(String name, String code, String symbol, boolean isBase, BigDecimal rate) {

        if (!currencyRepository.existsByCode(code)) {
            CurrencyDTO currency = currencyService.createCurrency(
                    new CurrencyCreateDTO(
                            name,
                            code,
                            symbol,
                            isBase
                    )
            );

            exchangeRateService.createExchangeRate(new ExchangeRateCreateDTO(currency.getId(), rate));
        }

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
