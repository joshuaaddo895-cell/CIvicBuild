package backend.example.civicbuild.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderStatus;
import backend.example.civicbuild.order.repository.OrderRepository;
import backend.example.civicbuild.payment.client.PaystackClient;
import backend.example.civicbuild.payment.client.PaystackInitializeResponse;
import backend.example.civicbuild.payment.repository.PaymentEventRepository;
import backend.example.civicbuild.payment.security.PaystackSignatureVerifier;
import backend.example.civicbuild.support.IntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

class OrderPaymentIntegrationTest extends IntegrationTestBase {

    private static final String VALID_PASSWORD = "Secret123";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Autowired
    private PaystackSignatureVerifier signatureVerifier;

    @MockitoBean
    private PaystackClient paystackClient;

    @Test
    void checkout_webhookSuccess_marksOrderPaid() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);

        when(paystackClient.initializeTransaction(anyString(), anyLong(), anyString(), eq("GHS")))
                .thenReturn(new PaystackInitializeResponse(
                        true,
                        "ok",
                        new PaystackInitializeResponse.PaystackInitializeData(
                                "https://checkout.paystack.com/test", "access-code", "CB-test")));

        Map<String, Object> checkoutData = checkout(email, sampleCheckoutBody());
        String reference = (String) checkoutData.get("paystackReference");
        UUID orderId = UUID.fromString((String) checkoutData.get("orderId"));

        Order pending = orderRepository.findById(orderId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(pending.getTotal()).isEqualByComparingTo("200.00");

        String webhookBody = webhookPayload(reference, 20000L, 4099797291L);
        postWebhook(webhookBody, signatureVerifier.hmacSha512Hex(webhookBody.getBytes(StandardCharsets.UTF_8)));

        Order paid = orderRepository.findById(orderId).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentEventRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateWebhook_isIdempotent() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);
        when(paystackClient.initializeTransaction(anyString(), anyLong(), anyString(), eq("GHS")))
                .thenReturn(new PaystackInitializeResponse(
                        true,
                        "ok",
                        new PaystackInitializeResponse.PaystackInitializeData(
                                "https://checkout.paystack.com/test", "access-code", "CB-test")));

        Map<String, Object> checkoutData = checkout(email, sampleCheckoutBody());
        String reference = (String) checkoutData.get("paystackReference");
        UUID orderId = UUID.fromString((String) checkoutData.get("orderId"));

        String webhookBody = webhookPayload(reference, 20000L, 4099797292L);
        String signature = signatureVerifier.hmacSha512Hex(webhookBody.getBytes(StandardCharsets.UTF_8));
        postWebhook(webhookBody, signature);
        postWebhook(webhookBody, signature);

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentEventRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidWebhookSignature_rejected() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);
        when(paystackClient.initializeTransaction(anyString(), anyLong(), anyString(), eq("GHS")))
                .thenReturn(new PaystackInitializeResponse(
                        true,
                        "ok",
                        new PaystackInitializeResponse.PaystackInitializeData(
                                "https://checkout.paystack.com/test", "access-code", "CB-test")));

        Map<String, Object> checkoutData = checkout(email, sampleCheckoutBody());
        UUID orderId = UUID.fromString((String) checkoutData.get("orderId"));

        String webhookBody = webhookPayload((String) checkoutData.get("paystackReference"), 20000L, 4099797293L);
        ResponseEntity<String> response = rest.postForEntity(
                paymentUrl("/webhook"),
                new HttpEntity<>(webhookBody, webhookHeaders("invalid-signature")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentEventRepository.count()).isZero();
    }

    @Test
    void amountMismatch_doesNotMarkPaid() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);
        when(paystackClient.initializeTransaction(anyString(), anyLong(), anyString(), eq("GHS")))
                .thenReturn(new PaystackInitializeResponse(
                        true,
                        "ok",
                        new PaystackInitializeResponse.PaystackInitializeData(
                                "https://checkout.paystack.com/test", "access-code", "CB-test")));

        Map<String, Object> checkoutData = checkout(email, sampleCheckoutBody());
        String reference = (String) checkoutData.get("paystackReference");
        UUID orderId = UUID.fromString((String) checkoutData.get("orderId"));

        String webhookBody = webhookPayload(reference, 10000L, 4099797294L);
        postWebhook(webhookBody, signatureVerifier.hmacSha512Hex(webhookBody.getBytes(StandardCharsets.UTF_8)));

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    private Map<String, Object> checkout(String email, Map<String, Object> body) throws Exception {
        HttpHeaders headers = authHeaders(email);
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

    private HttpHeaders authHeaders(String email) throws Exception {
        ResponseEntity<String> loginResponse = rest.postForEntity(
                authUrl("/login"),
                new HttpEntity<>(Map.of("email", email, "password", VALID_PASSWORD), jsonHeaders()),
                String.class);
        ApiResponse<Map<String, Object>> loginBody =
                objectMapper.readValue(loginResponse.getBody(), new TypeReference<>() {});
        String accessToken = (String) loginBody.data().get("accessToken");

        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private void registerAndLogin(String email) {
        ResponseEntity<String> response = rest.postForEntity(
                authUrl("/register"),
                new HttpEntity<>(
                        Map.of("fullName", "Buyer", "email", email, "password", VALID_PASSWORD),
                        jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerifiedAt(java.time.Instant.now());
        userRepository.save(user);
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
        return "buyer-" + UUID.randomUUID() + "@example.com";
    }
}
