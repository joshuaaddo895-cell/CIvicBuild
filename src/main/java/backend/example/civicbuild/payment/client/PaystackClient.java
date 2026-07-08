package backend.example.civicbuild.payment.client;

import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.order.exception.CheckoutException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import backend.example.civicbuild.payment.exception.PaystackApiException;

@Component
public class PaystackClient {

    private static final Logger log = LoggerFactory.getLogger(PaystackClient.class);
    private static final String BASE_URL = "https://api.paystack.co";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String callbackUrl;

    public PaystackClient(AppProperties properties, ObjectMapper objectMapper) {
        String secretKey = properties.paystack().secretKey();
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("PAYSTACK_SECRET_KEY is not set");
        }
        log.info(
                "Paystack client configured (key prefix: {}, callback: {})",
                secretKey.length() >= 8 ? secretKey.substring(0, 8) + "..." : "[too-short]",
                properties.paystack().callbackUrl());

        this.objectMapper = objectMapper;
        this.callbackUrl = properties.paystack().callbackUrl();
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public PaystackInitializeResponse initializeTransaction(
            String email, long amountPesewas, String reference, String currency) {
        Map<String, Object> body = Map.of(
                "email", email,
                "amount", amountPesewas,
                "reference", reference,
                "currency", currency,
                "callback_url", callbackUrl);

        log.info(
                "Paystack initialize request: reference={}, email={}, amountPesewas={}, currency={}",
                reference,
                email,
                amountPesewas,
                currency);

        try {
            String rawResponse = restClient.post()
                    .uri("/transaction/initialize")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("Paystack initialize raw response for reference {}: {}", reference, rawResponse);

            if (!StringUtils.hasText(rawResponse)) {
                throw new CheckoutException("Payment initialization failed. Please try again.");
            }

            PaystackInitializeResponse response =
                    objectMapper.readValue(rawResponse, PaystackInitializeResponse.class);
            validateInitializeResponse(response, reference);
            return response;
        } catch (CheckoutException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error(
                    "Paystack initialize HTTP {} for reference {}: {}",
                    ex.getStatusCode().value(),
                    reference,
                    responseBody,
                    ex);
            if (ex.getStatusCode().value() == 401) {
                throw new CheckoutException("Payment provider authentication failed. Check Paystack secret key.");
            }
            throw new CheckoutException("Payment initialization failed. Please try again.");
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("Paystack initialize request failed for reference {}", reference, ex);
            throw new CheckoutException("Payment provider unavailable. Please try again later.");
        }
    }

    public PaystackVerifyResponse verifyTransaction(String reference) {
        try {
            String rawResponse = restClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .retrieve()
                    .body(String.class);

            log.debug("Paystack verify raw response for reference {}: {}", reference, rawResponse);

            if (!StringUtils.hasText(rawResponse)) {
                throw new PaystackApiException("Empty Paystack verify response");
            }

            PaystackVerifyResponse response = objectMapper.readValue(rawResponse, PaystackVerifyResponse.class);
            if (response == null) {
                throw new PaystackApiException("Empty Paystack verify response");
            }
            return response;
        } catch (PaystackApiException ex) {
            throw ex;
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("Paystack verify request failed for reference {}", reference, ex);
            throw new PaystackApiException("Payment verification unavailable");
        }
    }

    private void validateInitializeResponse(PaystackInitializeResponse response, String reference) {
        if (response == null || !response.status() || response.data() == null) {
            String message = response == null ? "Empty Paystack response" : response.message();
            log.error("Paystack initialize rejected for reference {}: {}", reference, message);
            throw new CheckoutException("Payment initialization failed. Please try again.");
        }
        if (!StringUtils.hasText(response.data().authorizationUrl())) {
            log.error(
                    "Paystack initialize missing authorization_url for reference {} (message={})",
                    reference,
                    response.message());
            throw new CheckoutException("Payment initialization failed. Please try again.");
        }
    }
}
