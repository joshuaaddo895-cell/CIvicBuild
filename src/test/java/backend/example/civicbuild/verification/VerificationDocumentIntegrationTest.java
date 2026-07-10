package backend.example.civicbuild.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.storage.StorageService;
import backend.example.civicbuild.storage.StoredFile;
import backend.example.civicbuild.support.IntegrationTestBase;
import backend.example.civicbuild.verification.entity.VerificationDocumentType;
import backend.example.civicbuild.verification.repository.VerificationDocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class VerificationDocumentIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "Secret123";
    private static final byte[] PNG_BYTES =
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationDocumentRepository verificationDocumentRepository;

    @MockitoBean
    private StorageService storageService;

    @Test
    void uploadDocument_storesMetadata_andSignedUrlWorks() throws Exception {
        String email = uniqueEmail();
        register(email);
        User user = setRole(email, Role.CONSTRUCTION_AGENCY);
        String accessToken = login(email);

        when(storageService.uploadPrivateDocument(any(), any(), any()))
                .thenReturn(new StoredFile("verification-docs/" + user.getId() + "/stored-id", "image", "png", null));
        when(storageService.generateSignedPrivateUrl(
                        eq("verification-docs/" + user.getId() + "/stored-id"), eq("image"), eq("png"), any(Duration.class)))
                .thenReturn("https://res.cloudinary.com/test/image/authenticated/signed");

        ResponseEntity<String> uploadResponse = rest.postForEntity(
                verificationUrl("/upload-document?documentType=BUSINESS_REGISTRATION"),
                multipartEntity(accessToken, PNG_BYTES, "registration.png"),
                String.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> uploadBody =
                objectMapper.readValue(uploadResponse.getBody(), new TypeReference<>() {});
        assertThat(uploadBody.data().get("publicId")).isEqualTo("verification-docs/" + user.getId() + "/stored-id");
        assertThat(uploadBody.data().get("resourceType")).isEqualTo("image");

        assertThat(verificationDocumentRepository.findByUserIdAndDocumentType(
                        user.getId(), VerificationDocumentType.BUSINESS_REGISTRATION))
                .isPresent();

        ResponseEntity<String> urlResponse = rest.exchange(
                verificationUrl("/" + user.getId() + "/document-url?documentType=BUSINESS_REGISTRATION"),
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(accessToken)),
                String.class);

        assertThat(urlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> urlBody =
                objectMapper.readValue(urlResponse.getBody(), new TypeReference<>() {});
        assertThat(urlBody.data().get("signedUrl"))
                .isEqualTo("https://res.cloudinary.com/test/image/authenticated/signed");
    }

    @Test
    void uploadDocument_allowedForAnyAuthenticatedRole_includingCustomer() throws Exception {
        String email = uniqueEmail();
        register(email);
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);

        when(storageService.uploadPrivateDocument(any(), any(), any()))
                .thenReturn(new StoredFile("verification-docs/" + user.getId() + "/stored-id", "image", "png", null));

        String accessToken = login(email);
        ResponseEntity<String> response = rest.postForEntity(
                verificationUrl("/upload-document?documentType=GOVERNMENT_ID"),
                multipartEntity(accessToken, PNG_BYTES, "id.png"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getDocumentUrl_forbiddenForOtherUser() throws Exception {
        String ownerEmail = uniqueEmail();
        String otherEmail = uniqueEmail();
        register(ownerEmail);
        register(otherEmail);
        User owner = setRole(ownerEmail, Role.DELIVERY_PROVIDER);
        setRole(otherEmail, Role.DELIVERY_PROVIDER);

        when(storageService.uploadPrivateDocument(any(), any(), any()))
                .thenReturn(new StoredFile("verification-docs/" + owner.getId() + "/stored-id", "image", "png", null));

        String ownerToken = login(ownerEmail);
        rest.postForEntity(
                verificationUrl("/upload-document?documentType=GOVERNMENT_ID"),
                multipartEntity(ownerToken, PNG_BYTES, "id.png"),
                String.class);

        String otherToken = login(otherEmail);
        ResponseEntity<String> response = rest.exchange(
                verificationUrl("/" + owner.getId() + "/document-url?documentType=GOVERNMENT_ID"),
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(otherToken)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void register(String email) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(Map.of("fullName", "Test User", "email", email, "password", PASSWORD), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private User setRole(String email, Role role) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    @SuppressWarnings("unchecked")
    private String login(String email) throws Exception {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", PASSWORD), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return (String) body.data().get("accessToken");
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartEntity(
            String accessToken, byte[] bytes, String filename) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        return new HttpEntity<>(body, headers);
    }

    private String uniqueEmail() {
        return "verify-" + UUID.randomUUID() + "@example.com";
    }
}
