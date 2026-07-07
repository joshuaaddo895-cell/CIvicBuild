package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.dto.AuthResponse;
import backend.example.civicbuild.auth.dto.LoginRequest;
import backend.example.civicbuild.auth.dto.RegisterRequest;
import backend.example.civicbuild.auth.dto.UserResponse;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.entity.VerificationStatus;
import backend.example.civicbuild.auth.exception.AccountInactiveException;
import backend.example.civicbuild.auth.exception.DuplicateEmailException;
import backend.example.civicbuild.auth.exception.GoogleOnlyAccountException;
import backend.example.civicbuild.auth.exception.InvalidCredentialsException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.JwtService;
import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.email.service.EmailService;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication use-cases: registration, login, token refresh, and logout.
 * Business rules live here; the controller only adapts HTTP to these methods.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final long accessTokenTtlSeconds;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, TokenService tokenService, EmailService emailService,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.accessTokenTtlSeconds = properties.jwt().accessTokenTtl().toSeconds();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .active(true)
                .build();
        User saved = userRepository.saveAndFlush(user);
        log.info("Registered new user id={}", saved.getId());

        // Best-effort, async: an email hiccup must not fail registration.
        emailService.sendWelcomeEmail(saved);

        return UserResponse.from(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() == null) {
            throw new GoogleOnlyAccountException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        TokenService.RotationResult rotation = tokenService.rotate(rawRefreshToken);
        String accessToken = jwtService.generateAccessToken(rotation.user());
        return AuthResponse.bearer(accessToken, rotation.rawRefreshToken(), accessTokenTtlSeconds);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revoke(rawRefreshToken);
    }

    /** Issues our JWT access + refresh tokens for an already-authenticated user. */
    @Transactional
    public AuthResponse issueTokensForUser(User user) {
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = tokenService.issueRefreshToken(user);
        return AuthResponse.bearer(accessToken, refreshToken, accessTokenTtlSeconds);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
