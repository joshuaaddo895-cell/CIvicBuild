package backend.example.civicbuild.payment.client;

public record PaystackInitializeResponse(
        boolean status,
        String message,
        PaystackInitializeData data) {

    public record PaystackInitializeData(
            String authorizationUrl,
            String accessCode,
            String reference) {}
}
