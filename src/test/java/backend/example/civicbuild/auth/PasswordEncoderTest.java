package backend.example.civicbuild.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Test
    void bcrypt_hashesAndVerifiesPassword() {
        String raw = "Secret123";

        String hash = encoder.encode(raw);

        assertThat(hash).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hash)).isTrue();
        assertThat(encoder.matches("WrongPass1", hash)).isFalse();
    }

    @Test
    void bcrypt_producesDifferentHashesForSameInput() {
        String raw = "Secret123";

        assertThat(encoder.encode(raw)).isNotEqualTo(encoder.encode(raw));
    }
}
