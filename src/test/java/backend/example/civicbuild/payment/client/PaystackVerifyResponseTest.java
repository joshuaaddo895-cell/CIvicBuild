package backend.example.civicbuild.payment.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaystackVerifyResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void deserializesFullPaystackVerifyPayload() throws Exception {
        String json = """
                {
                  "status": true,
                  "message": "Verification successful",
                  "data": {
                    "id": 4099260516,
                    "domain": "test",
                    "status": "success",
                    "reference": "CB-test-ref",
                    "amount": 20000,
                    "currency": "GHS",
                    "paid_at": "2024-08-22T09:15:02.000Z",
                    "gateway_response": "Successful",
                    "channel": "card",
                    "customer": {"email": "buyer@example.com"}
                  }
                }
                """;

        PaystackVerifyResponse response = objectMapper.readValue(json, PaystackVerifyResponse.class);

        assertThat(response.status()).isTrue();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().status()).isEqualTo("success");
        assertThat(response.data().amount()).isEqualTo(20000L);
        assertThat(response.data().currency()).isEqualTo("GHS");
    }
}
