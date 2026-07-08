package backend.example.civicbuild.payment.security;

import static org.assertj.core.api.Assertions.assertThat;

import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.config.TestAppProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaystackSignatureVerifierTest {

    private PaystackSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        AppProperties properties = TestAppProperties.defaults();
        verifier = new PaystackSignatureVerifier(properties);
    }

    @Test
    void validSignature_passes() {
        byte[] body = "{\"event\":\"charge.success\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.hmacSha512Hex(body);
        assertThat(verifier.isValid(body, signature)).isTrue();
    }

    @Test
    void tamperedPayload_fails() {
        byte[] body = "{\"event\":\"charge.success\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.hmacSha512Hex(body);
        byte[] tampered = "{\"event\":\"charge.failed\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(tampered, signature)).isFalse();
    }

    @Test
    void wrongSecret_fails() {
        byte[] body = "{\"event\":\"charge.success\"}".getBytes(StandardCharsets.UTF_8);
        PaystackSignatureVerifier other = new PaystackSignatureVerifier(new AppProperties(
                TestAppProperties.defaults().jwt(),
                TestAppProperties.defaults().passwordReset(),
                TestAppProperties.defaults().rateLimit(),
                TestAppProperties.defaults().email(),
                TestAppProperties.TEST_GOOGLE,
                new AppProperties.Paystack(
                        "sk_test_different_secret",
                        "pk_test",
                        "http://localhost/callback",
                        "http://localhost/webhook",
                        false)));
        String signature = other.hmacSha512Hex(body);
        assertThat(verifier.isValid(body, signature)).isFalse();
    }
}
