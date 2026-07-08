package backend.example.civicbuild.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.auth.entity.PasswordResetToken;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.PasswordResetTokenRepository;
import backend.example.civicbuild.auth.repository.RefreshTokenRepository;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.TokenHasher;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.order.repository.OrderRepository;
import backend.example.civicbuild.payment.client.PaystackClient;
import backend.example.civicbuild.payment.client.PaystackInitializeResponse;
import backend.example.civicbuild.payment.entity.PaymentEvent;
import backend.example.civicbuild.payment.repository.PaymentEventRepository;
import backend.example.civicbuild.payment.security.PaystackSignatureVerifier;
import backend.example.civicbuild.support.IntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class UserDeleteCascadeIntegrationTest extends IntegrationTestBase {

    private static final String VALID_PASSWORD = "Secret123";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Autowired
    private PaystackSignatureVerifier signatureVerifier;

    @Autowired
    private TokenHasher tokenHasher;

    @MockitoBean
    private PaystackClient paystackClient;

    @Test
    void deleteAccount_cascadesOwnedRows() throws Exception {
        String email = uniqueEmail();
        register(email);
        Map<String, Object> loginData = login(email);
        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();

        User user = userRepository.findById(userId).orElseThrow();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash("reset-" + userId))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        when(paystackClient.initializeTransaction(anyString(), anyLong(), anyString(), eq("GHS")))
                .thenReturn(new PaystackInitializeResponse(
                        true,
                        "ok",
                        new PaystackInitializeResponse.PaystackInitializeData(
                                "https://checkout.paystack.com/test", "access-code", "CB-test")));

        Map<String, Object> checkoutData = checkout(email, loginData, sampleCheckoutBody());
        UUID orderId = UUID.fromString((String) checkoutData.get("orderId"));
        String reference = (String) checkoutData.get("paystackReference");

        String webhookBody = webhookPayload(reference, 20000L, 4099797399L);
        postWebhook(webhookBody, signatureVerifier.hmacSha512Hex(webhookBody.getBytes(StandardCharsets.UTF_8)));

        assertThat(userRepository.existsById(userId)).isTrue();
        assertThat(refreshTokenRepository.count()).isPositive();
        assertThat(passwordResetTokenRepository.count()).isPositive();
        assertThat(orderRepository.existsById(orderId)).isTrue();
        assertThat(paymentEventRepository.count()).isEqualTo(1);
        PaymentEvent event = paymentEventRepository.findAll().getFirst();
        assertThat(event.getOrderId()).isEqualTo(orderId);

        deleteAccount(loginData);

        assertThat(userRepository.existsById(userId)).isFalse();
        assertThat(refreshTokenRepository.count()).isZero();
        assertThat(passwordResetTokenRepository.count()).isZero();
        assertThat(orderRepository.existsById(orderId)).isFalse();
        assertThat(paymentEventRepository.count()).isZero();
    }

    private void register(String email) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(
                        Map.of("fullName", "Cascade User", "email", email, "password", VALID_PASSWORD),
                        jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);
    }

    private Map<String, Object> login(String email) throws Exception {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", VALID_PASSWORD), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Map<String, Object>> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return body.data();
    }

    private void deleteAccount(Map<String, Object> loginData) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth((String) loginData.get("accessToken"));
        ResponseEntity<String> response = rest.exchange(
                accountUrl(""),
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Map<String, Object> checkout(String email, Map<String, Object> loginData, Map<String, Object> body)
            throws Exception {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth((String) loginData.get("accessToken"));
        ResponseEntity<String> response = rest.postForEntity(
                orderUrl("/checkout"), new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<Map<String, Object>> parsed =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return parsed.data();
    }

    private void postWebhook(String body, String signature) {
        ResponseEntity<String> response = rest.postForEntity(
                paymentUrl("/webhook"), new HttpEntity<>(body, webhookHeaders(signature)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders webhookHeaders(String signature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-paystack-signature", signature);
        return headers;
    }

    private Map<String, Object> sampleCheckoutBody() {
        return Map.of(
                "items",
                List.of(Map.of(
                        "productName", "Cement",
                        "supplierName", "BuildCo",
                        "unitPrice", 100,
                        "quantity", 2,
                        "unit", "bags")),
                "delivery",
                Map.of(
                        "address", "12 Market Road",
                        "city", "Accra",
                        "region", "Greater Accra",
                        "phoneNumber", "+233201234567"));
    }

    private String webhookPayload(String reference, long amountPesewas, long transactionId) {
        return """
                {"event":"charge.success","data":{"id":%d,"reference":"%s","amount":%d,"currency":"GHS","status":"success"}}
                """
                .formatted(transactionId, reference, amountPesewas)
                .trim();
    }

    private String uniqueEmail() {
        return "cascade-" + UUID.randomUUID() + "@example.com";
    }
}
