package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.entity.RefreshToken;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.InvalidTokenException;
import backend.example.civicbuild.auth.repository.RefreshTokenRepository;
import backend.example.civicbuild.auth.security.TokenHasher;
import backend.example.civicbuild.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the lifecycle of opaque refresh tokens: issuance, validation, rotation and revocation.
 * Only hashes are persisted; the raw token is returned to the caller exactly once at issuance.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final AppProperties properties;
    private final Clock clock;

    public TokenService(RefreshTokenRepository refreshTokenRepository, TokenHasher tokenHasher,
            AppProperties properties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.properties = properties;
        this.clock = clock;
    }

    /** Issues a new refresh token for the user and returns the raw (unhashed) value. */
    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = tokenHasher.generateToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash(rawToken))
                .expiresAt(clock.instant().plus(properties.jwt().refreshTokenTtl()))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Rotates a refresh token: validates the presented raw token, revokes it, and issues a fresh
     * one for the same user. Returns the new raw token together with the owning user.
     */
    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken current = validateActive(rawToken);
        current.setRevokedAt(clock.instant());
        User user = current.getUser();
        String newRawToken = issueRefreshToken(user);
        return new RotationResult(user, newRawToken);
    }

    /** Revokes the refresh token matching the presented raw value. Idempotent and never leaks existence. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevokedAt(clock.instant());
                    }
                });
    }

    private RefreshToken validateActive(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));
        if (!token.isActive(clock.instant())) {
            log.debug("Refresh token rejected (revoked or expired)");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        return token;
    }

    public record RotationResult(User user, String rawRefreshToken) {}
}
