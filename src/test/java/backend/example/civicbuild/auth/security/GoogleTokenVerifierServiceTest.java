package backend.example.civicbuild.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.example.civicbuild.auth.exception.InvalidGoogleTokenException;
import backend.example.civicbuild.config.TestAppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleTokenVerifierServiceTest {

    private GoogleTokenVerifierService verifier;

    @BeforeEach
    void setUp() {
        verifier = new GoogleTokenVerifierService(TestAppProperties.defaults());
    }

    @Test
    void verify_rejectsBlankToken() {
        assertThatThrownBy(() -> verifier.verify("  "))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void verify_rejectsMalformedToken() {
        assertThatThrownBy(() -> verifier.verify("not-a-real-jwt"))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }
}
