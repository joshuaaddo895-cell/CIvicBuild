package backend.example.civicbuild.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import backend.example.civicbuild.auth.entity.PasswordResetToken;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.PasswordResetTokenRepository;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.TokenHasher;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.support.IntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthIntegrationTest extends IntegrationTestBase {

    private static final String VALID_PASSWORD = "Secret123";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TokenHasher tokenHasher;

    @Test
    void registerLoginRefreshLogout_happyPath() throws Exception {
        String email = uniqueEmail();

        register(email, "Jane Doe", VALID_PASSWORD);

        Map<String, Object> loginData = login(email, VALID_PASSWORD);
        String refreshToken = (String) loginData.get("refreshToken");
        assertThat(refreshToken).isNotBlank();

        Map<String, Object> refreshed = refresh(refreshToken);
        String newRefreshToken = (String) refreshed.get("refreshToken");
        assertThat(newRefreshToken).isNotBlank().isNotEqualTo(refreshToken);

        refreshExpectingUnauthorized(refreshToken);

        logout(newRefreshToken);
        refreshExpectingUnauthorized(newRefreshToken);
    }

    @Test
    void register_rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        register(email, "First User", VALID_PASSWORD);

        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(
                        Map.of("fullName", "Second User", "email", email, "password", VALID_PASSWORD),
                        jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiResponse<Void> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(body.success()).isFalse();
        assertThat(body.message()).isEqualTo("An account with this email already exists");
    }

    @Test
    void login_rejectsWrongPassword() throws Exception {
        String email = uniqueEmail();
        register(email, "Jane Doe", VALID_PASSWORD);

        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", "WrongPass1"), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ApiResponse<Void> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(body.message()).isEqualTo("Invalid email or password");
    }

    @Test
    void login_rejectsInactiveAccount() throws Exception {
        String email = uniqueEmail();
        register(email, "Inactive User", VALID_PASSWORD);

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", VALID_PASSWORD), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ApiResponse<Void> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(body.message()).isEqualTo("This account has been deactivated. Please contact support.");
    }

    @Test
    void login_rateLimitsAfterRepeatedAttempts() {
        String email = uniqueEmail();
        register(email, "Rate Limited", VALID_PASSWORD);
        HttpHeaders headers = jsonHeaders();
        headers.set("X-Forwarded-For", "203.0.113.10");

        for (int attempt = 0; attempt < 5; attempt++) {
            ResponseEntity<String> response = rest.postForEntity(
                    authUrl("/login"),
                    new HttpEntity<>(Map.of("email", email, "password", "WrongPass1"), headers),
                    String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<String> blocked = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", "WrongPass1"), headers),
                String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void forgotPassword_alwaysReturnsGenericSuccess() {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/forgot-password"),
                new HttpEntity<>(Map.of("email", uniqueEmail()), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void resetPassword_rejectsExpiredAndReusedTokens() throws Exception {
        String email = uniqueEmail();
        register(email, "Reset User", VALID_PASSWORD);
        User user = userRepository.findByEmail(email).orElseThrow();

        String expiredRaw = tokenHasher.generateToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash(expiredRaw))
                .expiresAt(Instant.now().minusSeconds(60))
                .build());

        resetPasswordExpectingUnauthorized(expiredRaw, "NewSecret456");

        rest.postForEntity(
                authUrl("/forgot-password"),
                new HttpEntity<>(Map.of("email", email), jsonHeaders()),
                String.class);

        ArgumentCaptor<String> resetLinkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(user), resetLinkCaptor.capture());
        String rawToken = extractTokenFromResetLink(resetLinkCaptor.getValue());
        String newPassword = "ResetPass789";

        ResponseEntity<String> reset = rest.postForEntity(
                authUrl("/reset-password"),
                new HttpEntity<>(Map.of("token", rawToken, "newPassword", newPassword), jsonHeaders()),
                String.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);

        resetPasswordExpectingUnauthorized(rawToken, "AnotherPass1");

        loginExpectingUnauthorized(email, VALID_PASSWORD);
        login(email, newPassword);
    }

    private void register(String email, String fullName, String password) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(Map.of("fullName", fullName, "email", email, "password", password), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(emailService).sendWelcomeEmail(any(User.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> login(String email, String password) throws Exception {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return body.data();
    }

    private void loginExpectingUnauthorized(String email, String password) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> refresh(String refreshToken) throws Exception {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/refresh"),
                new HttpEntity<>(Map.of("refreshToken", refreshToken), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return body.data();
    }

    private void refreshExpectingUnauthorized(String refreshToken) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/refresh"),
                new HttpEntity<>(Map.of("refreshToken", refreshToken), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void logout(String refreshToken) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/logout"),
                new HttpEntity<>(Map.of("refreshToken", refreshToken), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void resetPasswordExpectingUnauthorized(String token, String newPassword) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/reset-password"),
                new HttpEntity<>(Map.of("token", token, "newPassword", newPassword), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String extractTokenFromResetLink(String resetLink) {
        int tokenIndex = resetLink.indexOf("token=");
        assertThat(tokenIndex).isGreaterThanOrEqualTo(0);
        return resetLink.substring(tokenIndex + "token=".length());
    }
}
