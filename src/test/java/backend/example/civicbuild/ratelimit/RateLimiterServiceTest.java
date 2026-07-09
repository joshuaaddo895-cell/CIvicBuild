package backend.example.civicbuild.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.config.TestAppProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        AppProperties properties = TestAppProperties.defaults();
        rateLimiter = new RateLimiterService(redisTemplate, properties);
    }

    @Test
    void checkAndConsume_allowsRequestsUnderLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any())).thenReturn(3L);

        assertThatCode(() -> rateLimiter.checkAndConsume("login", "127.0.0.1", "user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndConsume_setsExpiryOnFirstAttempt() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any())).thenReturn(1L);

        rateLimiter.checkAndConsume("login", "127.0.0.1", "user@example.com");

        verify(redisTemplate).expire(any(), eq(Duration.ofMinutes(15)));
    }

    @Test
    void checkAndConsume_throwsWhenLimitExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any())).thenReturn(6L);

        assertThatThrownBy(() -> rateLimiter.checkAndConsume("login", "127.0.0.1", "user@example.com"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkAndConsume_failsOpenWhenRedisUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new DataAccessResourceFailureException("down"));

        assertThatCode(() -> rateLimiter.checkAndConsume("login", "127.0.0.1", "user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndConsume_skipsWhenDisabled() {
        AppProperties disabled = new AppProperties(
                new AppProperties.NotNullJwt("secret", "issuer", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.PasswordReset(Duration.ofMinutes(30)),
                new AppProperties.RateLimit(false, 5, Duration.ofMinutes(15)),
                new AppProperties.Email("re_test", "test@example.com", "http://localhost/reset"),
                TestAppProperties.TEST_GOOGLE,
                TestAppProperties.TEST_CLOUDINARY,
                TestAppProperties.TEST_PAYSTACK);
        RateLimiterService disabledLimiter = new RateLimiterService(redisTemplate, disabled);

        disabledLimiter.checkAndConsume("login", "127.0.0.1", "user@example.com");

        verify(redisTemplate, never()).opsForValue();
    }
}
