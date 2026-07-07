package backend.example.civicbuild.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides an injectable {@link Clock} so time-dependent logic (token expiry, revocation) can be
 * deterministically tested rather than reading {@code Instant.now()} directly.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
