package backend.example.civicbuild.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Strongly-typed, validated binding for all {@code app.*} configuration.
 * Centralising these keeps magic strings out of the code and fails fast at startup
 * if a required value (e.g. the JWT secret) is missing.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        NotNullJwt jwt,
        PasswordReset passwordReset,
        RateLimit rateLimit,
        Email email,
        Google google,
        Paystack paystack) {

    public record NotNullJwt(
            @NotBlank String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl) {}

    public record PasswordReset(@NotNull Duration tokenTtl) {}

    public record RateLimit(
            boolean enabled,
            @Min(1) int maxAttempts,
            @NotNull Duration window) {}

    public record Email(
            @NotBlank String resendApiKey,
            @NotBlank String from,
            @NotBlank String resetBaseUrl) {}

    /** Google OAuth — only webClientId is required for ID-token verification. */
    public record Google(@NotBlank String webClientId) {}

    public record Paystack(
            @NotBlank String secretKey,
            @NotBlank String publicKey,
            @NotBlank String callbackUrl,
            @NotBlank String webhookUrl,
            boolean webhookIpCheckEnabled) {}
}
