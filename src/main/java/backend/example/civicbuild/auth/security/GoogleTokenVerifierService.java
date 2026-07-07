package backend.example.civicbuild.auth.security;

import backend.example.civicbuild.auth.exception.InvalidGoogleTokenException;
import backend.example.civicbuild.config.AppProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Verifies Google ID tokens against Google's public keys. Only {@link #GOOGLE_WEB_CLIENT_ID}
 * is required — the client secret is NOT used for ID-token verification.
 */
@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(AppProperties properties) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(properties.google().webClientId()))
                .build();
    }

    /**
     * Verifies signature, audience, issuer, and expiry, then returns trusted profile claims.
     *
     * @throws InvalidGoogleTokenException if the token is missing, malformed, or fails verification.
     */
    public VerifiedGoogleProfile verify(String rawIdToken) {
        if (rawIdToken == null || rawIdToken.isBlank()) {
            throw new InvalidGoogleTokenException();
        }
        try {
            GoogleIdToken idToken = verifier.verify(rawIdToken.trim());
            if (idToken == null) {
                log.debug("Google ID token verification returned null");
                throw new InvalidGoogleTokenException();
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                log.debug("Google ID token missing email claim");
                throw new InvalidGoogleTokenException();
            }
            String fullName = payload.get("name") != null ? payload.get("name").toString() : email;
            String pictureUrl = payload.get("picture") != null ? payload.get("picture").toString() : null;
            return new VerifiedGoogleProfile(
                    email.trim().toLowerCase(Locale.ROOT),
                    fullName,
                    pictureUrl);
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Google ID token verification failed: {}", e.getClass().getSimpleName());
            throw new InvalidGoogleTokenException();
        }
    }
}
