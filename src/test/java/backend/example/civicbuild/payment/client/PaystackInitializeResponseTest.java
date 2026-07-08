package backend.example.civicbuild.payment.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PaystackInitializeResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesPaystackSnakeCasePayload() throws Exception {
        String json = """
                {
                  "status": true,
                  "message": "Authorization URL created",
                  "data": {
                    "authorization_url": "https://checkout.paystack.com/abc",
                    "access_code": "code123",
                    "reference": "CB-test"
                  }
                }
                """;

        PaystackInitializeResponse response = objectMapper.readValue(json, PaystackInitializeResponse.class);

        assertThat(response.status()).isTrue();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().authorizationUrl()).isEqualTo("https://checkout.paystack.com/abc");
        assertThat(response.data().accessCode()).isEqualTo("code123");
        assertThat(response.data().reference()).isEqualTo("CB-test");
    }
}
