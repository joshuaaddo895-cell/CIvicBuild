package backend.example.civicbuild.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceKeyResolutionTest {

    @Test
    void resolveSigningKeyBytes_decodesBase64Secret() {
        byte[] raw = "dev-only-civicbuild-jwt-secret-change-me-in-production-1234567890"
                .getBytes();
        String base64 = Base64.getEncoder().encodeToString(raw);

        byte[] resolved = JwtService.resolveSigningKeyBytes(base64);

        assertThat(resolved).isEqualTo(raw);
    }

    @Test
    void resolveSigningKeyBytes_acceptsLongPlainTextSecret() {
        String secret = "plain-text-jwt-secret-that-is-at-least-32-bytes";

        byte[] resolved = JwtService.resolveSigningKeyBytes(secret);

        assertThat(resolved).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void resolveSigningKeyBytes_rejectsShortSecret() {
        assertThatThrownBy(() -> JwtService.resolveSigningKeyBytes("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is missing or invalid");
    }
}
