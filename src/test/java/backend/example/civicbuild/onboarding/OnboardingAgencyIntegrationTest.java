package backend.example.civicbuild.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.support.IntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OnboardingAgencyIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "Secret123";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAgency_usesDatabaseRole_afterOnboardingPatchWithoutTokenRefresh() throws Exception {
        String email = uniqueEmail();
        register(email);
        String customerToken = login(email);

        ResponseEntity<String> patchResponse = rest.exchange(
                usersUrl("/me/onboarding"),
                HttpMethod.PATCH,
                new HttpEntity<>(
                        "{\"accountType\":\"construction\"}",
                        jsonBearerHeaders(customerToken)),
                String.class);
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> createResponse = rest.postForEntity(
                agenciesUrl(""),
                new HttpEntity<>(
                        "{\"name\":\"BuildStrong Ltd\",\"category\":\"general-contracting\"}",
                        jsonBearerHeaders(customerToken)),
                String.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<Map<String, Object>> body =
                objectMapper.readValue(createResponse.getBody(), new TypeReference<>() {});
        assertThat(body.success()).isTrue();
        assertThat(body.data().get("name")).isEqualTo("BuildStrong Ltd");
    }

    private void register(String email) {
        rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(
                        "{\"fullName\":\"Agency Test\",\"email\":\""
                                + email + "\",\"password\":\"" + PASSWORD + "\"}",
                        jsonHeaders()),
                String.class);
    }

    private String login(String email) throws Exception {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(
                        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}", jsonHeaders()),
                String.class);
        ApiResponse<Map<String, Object>> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.data();
        return (String) data.get("accessToken");
    }

    private org.springframework.http.HttpHeaders jsonBearerHeaders(String token) {
        org.springframework.http.HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private String uniqueEmail() {
        return "agency-onboarding-" + System.nanoTime() + "@example.com";
    }
}
