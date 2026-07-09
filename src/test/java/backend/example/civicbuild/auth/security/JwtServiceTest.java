package backend.example.civicbuild.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.entity.VerificationStatus;
import backend.example.civicbuild.auth.exception.InvalidTokenException;
import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.config.TestAppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        AppProperties properties = TestAppProperties.defaults();
        jwtService = new JwtService(properties, FIXED_CLOCK);
        user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("hash")
                .role(Role.CUSTOMER)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .active(true)
                .build();
    }

    @Test
    void generateAndParseAccessToken_roundTripsClaims() {
        String token = jwtService.generateAccessToken(user);

        AccessTokenClaims claims = jwtService.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.email()).isEqualTo(user.getEmail());
        assertThat(claims.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void parseAccessToken_rejectsExpiredToken() {
        AppProperties properties = new AppProperties(
                new AppProperties.NotNullJwt(
                        "unit-test-jwt-secret-at-least-32-bytes",
                        "civicbuild-test",
                        Duration.ofSeconds(1),
                        Duration.ofDays(7)),
                new AppProperties.PasswordReset(Duration.ofMinutes(30)),
                new AppProperties.RateLimit(true, 5, Duration.ofMinutes(15)),
                new AppProperties.Email("re_test", "test@example.com", "http://localhost/reset"),
                TestAppProperties.TEST_GOOGLE,
                TestAppProperties.TEST_CLOUDINARY,
                TestAppProperties.TEST_PAYSTACK);
        JwtService shortLived = new JwtService(properties, FIXED_CLOCK);
        String token = shortLived.generateAccessToken(user);

        Clock afterExpiry = Clock.fixed(FIXED_NOW.plusSeconds(5), ZoneOffset.UTC);
        JwtService validator = new JwtService(properties, afterExpiry);

        assertThatThrownBy(() -> validator.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void parseAccessToken_rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.parseAccessToken(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }
}
