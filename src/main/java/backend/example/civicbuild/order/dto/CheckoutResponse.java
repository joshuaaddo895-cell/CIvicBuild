package backend.example.civicbuild.order.dto;

import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        String paystackReference,
        String authorizationUrl) {}
