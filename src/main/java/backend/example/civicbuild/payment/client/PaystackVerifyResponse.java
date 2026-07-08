package backend.example.civicbuild.payment.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaystackVerifyResponse(
        boolean status,
        String message,
        PaystackVerifyData data) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PaystackVerifyData(
            String status,
            String reference,
            long amount,
            String currency) {}
}
