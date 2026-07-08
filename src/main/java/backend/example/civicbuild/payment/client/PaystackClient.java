package backend.example.civicbuild.payment.client;

import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.order.exception.CheckoutException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import backend.example.civicbuild.payment.exception.PaystackApiException;

@Component
public class PaystackClient {

    private static final Logger log = LoggerFactory.getLogger(PaystackClient.class);
    private static final String BASE_URL = "https://api.paystack.co";

    private final RestClient restClient;
    private final String callbackUrl;

    public PaystackClient(AppProperties properties) {
        String secretKey = properties.paystack().secretKey();
        this.callbackUrl = properties.paystack().callbackUrl();
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public PaystackInitializeResponse initializeTransaction(
            String email, long amountPesewas, String reference, String currency) {
        try {
            Map<String, Object> body = Map.of(
                    "email", email,
                    "amount", amountPesewas,
                    "reference", reference,
                    "currency", currency,
                    "callback_url", callbackUrl);
            PaystackInitializeResponse response = restClient.post()
                    .uri("/transaction/initialize")
                    .body(body)
                    .retrieve()
                    .body(PaystackInitializeResponse.class);
            if (response == null || !response.status() || response.data() == null) {
                String message = response == null ? "Empty Paystack response" : response.message();
                log.error("Paystack initialize failed for reference {}: {}", reference, message);
                throw new CheckoutException("Payment initialization failed. Please try again.");
            }
            return response;
        } catch (RestClientException e) {
            log.error("Paystack initialize request failed for reference {}", reference, e);
            throw new CheckoutException("Payment provider unavailable. Please try again later.");
        }
    }

    public PaystackVerifyResponse verifyTransaction(String reference) {
        try {
            PaystackVerifyResponse response = restClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .retrieve()
                    .body(PaystackVerifyResponse.class);
            if (response == null) {
                throw new PaystackApiException("Empty Paystack verify response");
            }
            return response;
        } catch (RestClientException e) {
            log.error("Paystack verify request failed for reference {}", reference, e);
            throw new PaystackApiException("Payment verification unavailable");
        }
    }
}
