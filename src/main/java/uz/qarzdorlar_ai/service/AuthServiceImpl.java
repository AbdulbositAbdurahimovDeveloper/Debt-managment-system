package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.config.properties.JwtProperties;
import uz.qarzdorlar_ai.config.security.JwtService;
import uz.qarzdorlar_ai.exception.BadCredentialsException;
import uz.qarzdorlar_ai.exception.DataConflictException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.model.UserProfile;
import uz.qarzdorlar_ai.payload.TokenDTO;
import uz.qarzdorlar_ai.payload.user.LoginDTO;
import uz.qarzdorlar_ai.payload.user.RegistrationResponseDTO;
import uz.qarzdorlar_ai.payload.user.UserRegisterRequestDTO;
import uz.qarzdorlar_ai.payload.user.UserSummaryDTO;
import uz.qarzdorlar_ai.repository.UserProfileRepository;
import uz.qarzdorlar_ai.repository.UserRepository;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserProfileRepository userProfileRepository;


    @Override
    public TokenDTO login(LoginDTO loginDTO) {

        String password = loginDTO.getPassword();
        String username = loginDTO.getUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username : " + username));

        String encodedPassword = user.getPassword();

        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new BadCredentialsException("User password incorrected");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        TokenDTO tokenDto = TokenDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)

                .expiresIn(jwtProperties.getAccessTokenExpiration().toSeconds())
                .authorities(user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .username(user.getUsername())
                .build();

        log.info("User '{}' successfully authenticated and tokens generated.", username);

        return tokenDto;
    }


    @Override
    public RegistrationResponseDTO register(UserRegisterRequestDTO registerDTO) {

        if (userRepository.existsByUsername((registerDTO.getUsername()))) {
            throw new DataConflictException("Username already exists!");
        }
        if (userProfileRepository.existsByEmail((registerDTO.getEmail()))) {
            throw new DataConflictException("Email already registered!");
        }

        if (userProfileRepository.existsByPhoneNumber((registerDTO.getPhoneNumber()))) {
            throw new DataConflictException("Phone number already registered!");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName(registerDTO.getFirstName());
        userProfile.setLastName(registerDTO.getLastName());
        userProfile.setEmail(registerDTO.getEmail());
        userProfile.setPhoneNumber(registerDTO.getPhoneNumber());

        user.setUserProfile(userProfile);
        userProfile.setUser(user);

        userRepository.save(user);


        return new RegistrationResponseDTO(
                "Registered successfully",
                new UserSummaryDTO(
                        user.getId(),
                        user.getUsername(),
                        userProfile.getEmail()
                )
        );

    }
}
