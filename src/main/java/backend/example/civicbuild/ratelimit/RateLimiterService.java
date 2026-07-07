package backend.example.civicbuild.ratelimit;

import backend.example.civicbuild.config.AppProperties;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Fixed-window rate limiter backed by Redis.
 *
 * <p>The key combines action + client IP + email. Keying on the pair (rather than IP-only or
 * email-only) avoids two failure modes: IP-only would unfairly throttle many users behind one NAT,
 * while email-only would let an attacker lock a victim out by spamming their address. If Redis is
 * unavailable we fail OPEN (allow the request) and log — availability of auth is prioritised over
 * the brute-force protection layer, which is defence-in-depth rather than the primary control.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final AppProperties.RateLimit config;

    public RateLimiterService(StringRedisTemplate redisTemplate, AppProperties properties) {
        this.redisTemplate = redisTemplate;
        this.config = properties.rateLimit();
    }

    /**
     * Records an attempt and throws {@link RateLimitExceededException} once the configured
     * threshold within the window is exceeded.
     */
    public void checkAndConsume(String action, String ip, String email) {
        if (!config.enabled()) {
            return;
        }
        String key = buildKey(action, ip, email);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, config.window());
            }
            if (count != null && count > config.maxAttempts()) {
                log.warn("Rate limit exceeded for action '{}' (count={})", action, count);
                throw new RateLimitExceededException();
            }
        } catch (DataAccessException e) {
            // Fail open: don't block legitimate auth if the rate-limit store is down.
            log.error("Rate limiter unavailable; allowing request for action '{}'", action, e);
        }
    }

    private String buildKey(String action, String ip, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return "rl:%s:%s:%s".formatted(action, ip, normalizedEmail);
    }

    Duration window() {
        return config.window();
    }
}
