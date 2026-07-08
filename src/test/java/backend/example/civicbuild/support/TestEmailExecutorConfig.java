package backend.example.civicbuild.support;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Runs email tasks synchronously in tests so Mockito verifications are deterministic.
 */
@Configuration
@Profile("test")
public class TestEmailExecutorConfig {

    @Bean(name = "emailExecutor")
    Executor testEmailExecutor() {
        return Runnable::run;
    }
}
