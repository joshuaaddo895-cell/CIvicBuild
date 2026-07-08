package backend.example.civicbuild.auth;

import static org.assertj.core.api.Assertions.assertThat;

import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.support.IntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UserProfileIntegrationTest extends IntegrationTestBase {

    private static final String VALID_PASSWORD = "Secret123";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updateProfile_updatesFullNameAndProfilePicture() throws Exception {
        String email = uniqueEmail();
        register(email);
        Map<String, Object> loginData = login(email);
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth((String) loginData.get("accessToken"));

        ResponseEntity<String> response = rest.exchange(
                usersUrl("/me"),
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>(
                        Map.of(
                                "fullName", "Updated Name",
                                "profilePictureUrl", "https://cdn.example.com/avatar.jpg"),
                        headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(body.data().get("fullName")).isEqualTo("Updated Name");
        assertThat(body.data().get("profilePictureUrl")).isEqualTo("https://cdn.example.com/avatar.jpg");
        assertThat(body.data().get("email")).isEqualTo(email);
    }

    private void register(String email) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(
                        Map.of("fullName", "Profile User", "email", email, "password", VALID_PASSWORD),
                        jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private Map<String, Object> login(String email) throws Exception {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", VALID_PASSWORD), jsonHeaders()),
                String.class);
        ApiResponse<Map<String, Object>> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return body.data();
    }

    private String uniqueEmail() {
        return "profile-" + UUID.randomUUID() + "@example.com";
    }
}
