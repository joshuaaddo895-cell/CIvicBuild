package backend.example.civicbuild.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.GoogleTokenVerifierService;
import backend.example.civicbuild.auth.security.VerifiedGoogleProfile;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.support.IntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class GoogleAuthIntegrationTest extends IntegrationTestBase {

    private static final String MOCK_ID_TOKEN = "integration-mock-id-token";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GoogleTokenVerifierService googleTokenVerifier;

    @Test
    void googleSignIn_createsNewUserAndReturnsTokens() throws Exception {
        String email = "google-new-" + UUID.randomUUID() + "@example.com";
        when(googleTokenVerifier.verify(MOCK_ID_TOKEN))
                .thenReturn(new VerifiedGoogleProfile(email, "Google New User", "https://photo/url"));

        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/google"),
                new HttpEntity<>(Map.of("idToken", MOCK_ID_TOKEN), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(body.data().get("accessToken")).isNotNull();
        assertThat(body.data().get("refreshToken")).isNotNull();

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getProfilePictureUrl()).isEqualTo("https://photo/url");
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void googleSignIn_linksExistingManualAccount() throws Exception {
        String email = "google-link-" + UUID.randomUUID() + "@example.com";
        registerManualUser(email);

        when(googleTokenVerifier.verify(MOCK_ID_TOKEN))
                .thenReturn(new VerifiedGoogleProfile(email, "Google Linked", "https://linked/photo"));

        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/google"),
                new HttpEntity<>(Map.of("idToken", MOCK_ID_TOKEN), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(user.getProfilePictureUrl()).isEqualTo("https://linked/photo");
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void login_rejectsGoogleOnlyAccount() {
        String email = "google-only-" + UUID.randomUUID() + "@example.com";
        when(googleTokenVerifier.verify(anyString()))
                .thenReturn(new VerifiedGoogleProfile(email, "Google Only", null));

        rest.postForEntity(
                authUrl("/google"),
                new HttpEntity<>(Map.of("idToken", MOCK_ID_TOKEN), jsonHeaders()),
                String.class);

        ResponseEntity<String> login = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", "Secret123"), jsonHeaders()),
                String.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void registerManualUser(String email) {
        rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(Map.of(
                        "fullName", "Manual User",
                        "email", email,
                        "password", "Secret123"), jsonHeaders()),
                String.class);
    }
}
