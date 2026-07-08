package backend.example.civicbuild.payment.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaystackInitializeResponse(
        boolean status,
        String message,
        PaystackInitializeData data) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PaystackInitializeData(
            String authorizationUrl,
            String accessCode,
            String reference) {}
}
