package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.entity.PasswordResetToken;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.InvalidTokenException;
import backend.example.civicbuild.auth.repository.PasswordResetTokenRepository;
import backend.example.civicbuild.auth.repository.RefreshTokenRepository;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.TokenHasher;
import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.email.service.EmailService;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Password reset flow. Deliberately never reveals whether an email is registered (anti-enumeration):
 * the caller always sees a generic success. Reset tokens are single-use (guarded by {@code used_at})
 * and time-limited, and a successful reset revokes all refresh tokens to force re-login everywhere.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties properties;
    private final Clock clock;

    public PasswordResetService(UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository, TokenHasher tokenHasher,
            PasswordEncoder passwordEncoder, EmailService emailService, AppProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        // No 'else' branch: absence is silent to prevent user enumeration.
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant now = clock.instant();
            // Supersede any outstanding tokens so only the newest link is valid.
            passwordResetTokenRepository.invalidateOutstandingForUser(user.getId(), now);

            String rawToken = tokenHasher.generateToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHasher.hash(rawToken))
                    .expiresAt(now.plus(properties.passwordReset().tokenTtl()))
                    .build();
            passwordResetTokenRepository.save(token);

            emailService.sendPasswordResetEmail(user, buildResetLink(rawToken));
            log.info("Issued password reset token for user id={}", user.getId());
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        Instant now = clock.instant();
        if (!token.isConsumable(now)) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);

        // Standard practice: invalidate all sessions after a password change.
        int revoked = refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
        log.info("Password reset for user id={}; revoked {} active refresh token(s)", user.getId(), revoked);
    }

    private String buildResetLink(String rawToken) {
        return UriComponentsBuilder.fromUriString(properties.email().resetBaseUrl())
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
