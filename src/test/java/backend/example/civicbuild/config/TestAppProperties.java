package backend.example.civicbuild.config;

import java.time.Duration;

/** Shared test fixture values for {@link AppProperties}. */
public final class TestAppProperties {

    public static final AppProperties.Google TEST_GOOGLE =
            new AppProperties.Google("test-google-web-client-id.apps.googleusercontent.com");

    private TestAppProperties() {
    }

    public static final AppProperties.Paystack TEST_PAYSTACK = new AppProperties.Paystack(
            "sk_test_unit_test_secret_key",
            "pk_test_unit_test_public_key",
            "http://localhost:8081/api/payments/paystack/callback",
            "http://localhost:8081/api/payments/webhook",
            false);

    public static AppProperties defaults() {
        return new AppProperties(
                new AppProperties.NotNullJwt(
                        "unit-test-jwt-secret-at-least-32-bytes",
                        "civicbuild-test",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7)),
                new AppProperties.PasswordReset(Duration.ofMinutes(30)),
                new AppProperties.RateLimit(true, 5, Duration.ofMinutes(15)),
                new AppProperties.Email("re_test", "test@example.com", "http://localhost/reset"),
                TEST_GOOGLE,
                TEST_PAYSTACK);
    }
}
