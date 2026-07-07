package backend.example.civicbuild.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenHasherTest {

    private TokenHasher tokenHasher;

    @BeforeEach
    void setUp() {
        tokenHasher = new TokenHasher();
    }

    @Test
    void generateToken_producesUniqueUrlSafeValues() {
        String first = tokenHasher.generateToken();
        String second = tokenHasher.generateToken();

        assertThat(first).isNotBlank().doesNotContain("+", "/");
        assertThat(second).isNotBlank().isNotEqualTo(first);
    }

    @Test
    void hash_isDeterministicSha256Hex() {
        String raw = "sample-refresh-token-value";

        assertThat(tokenHasher.hash(raw)).isEqualTo(tokenHasher.hash(raw));
        assertThat(tokenHasher.hash(raw)).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hash_differsForDifferentInputs() {
        assertThat(tokenHasher.hash("token-a")).isNotEqualTo(tokenHasher.hash("token-b"));
    }
}
