package backend.example.civicbuild.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Generates opaque, high-entropy tokens and hashes them for at-rest storage.
 *
 * <p>Refresh and password-reset tokens are random secrets, not passwords, so a fast one-way hash
 * (SHA-256) is the correct choice — BCrypt's work factor is unnecessary here and its 72-byte input
 * limit / salting would prevent the direct hash lookups these flows rely on. The raw token is
 * returned to the client exactly once; only its hash is ever persisted.
 */
@Component
public class TokenHasher {

    private static final int TOKEN_BYTES = 32; // 256 bits of entropy
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

    /** Generates a new random, URL-safe opaque token. */
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }

    /** Deterministic SHA-256 hash (hex) of a raw token, used for storage and lookup. */
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM; this is unreachable.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
