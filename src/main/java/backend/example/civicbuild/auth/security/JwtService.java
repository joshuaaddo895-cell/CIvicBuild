package backend.example.civicbuild.auth.security;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.InvalidTokenException;
import backend.example.civicbuild.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Issues and validates our own <b>access</b> tokens (short-lived, stateless JWTs signed with HMAC).
 *
 * <p>Refresh tokens are intentionally NOT JWTs — they are opaque random secrets managed by
 * {@code TokenService} and validated against their stored hash, which is why this service only
 * deals with access tokens. Keeping issuance keyed off a {@link User} identity (not an auth
 * mechanism) means a future "Sign in with Google" flow can reuse it unchanged.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final AppProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(resolveSigningKeyBytes(properties.jwt().secret()));
    }

    /**
     * Accepts either a Base64-encoded key (recommended in {@code .env.example}) or a raw UTF-8
     * string of at least 32 bytes. Fails fast at startup if the configured secret is too weak.
     */
    static byte[] resolveSigningKeyBytes(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET is not configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret.trim());
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not Base64 — fall through to raw UTF-8 interpretation.
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 256 bits (32 bytes). "
                            + "Generate one with: openssl rand -base64 64");
        }
        return raw;
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        Instant expiry = now.plus(properties.jwt().accessTokenTtl());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.jwt().issuer())
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies signature, issuer and expiry, and returns the strongly-typed claims.
     *
     * @throws InvalidTokenException for any expired, tampered, or otherwise invalid token.
     */
    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.jwt().issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidTokenException("Invalid access token");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return new AccessTokenClaims(userId, email, role);
        } catch (ExpiredJwtException e) {
            log.debug("Access token expired");
            throw new InvalidTokenException("Access token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            // Covers malformed, tampered signature, unsupported, and bad claim values.
            log.debug("Access token rejected: {}", e.getClass().getSimpleName());
            throw new InvalidTokenException("Invalid access token");
        }
    }
}
