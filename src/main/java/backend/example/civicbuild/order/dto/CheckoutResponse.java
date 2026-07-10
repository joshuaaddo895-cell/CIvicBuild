package backend.example.civicbuild.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        String orderNumber,
        String paystackReference,
        String authorizationUrl,
        BigDecimal totalAmount) {}
