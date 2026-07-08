package backend.example.civicbuild.payment.client;

public record PaystackVerifyResponse(
        boolean status,
        String message,
        PaystackVerifyData data) {

    public record PaystackVerifyData(
            String status,
            String reference,
            long amount,
            String currency) {}
}
