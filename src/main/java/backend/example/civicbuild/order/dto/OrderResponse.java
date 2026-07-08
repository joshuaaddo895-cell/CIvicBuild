package backend.example.civicbuild.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String status,
        BigDecimal subtotal,
        BigDecimal total,
        String currency,
        String deliveryAddress,
        String deliveryCity,
        String deliveryRegion,
        String phoneNumber,
        String paystackReference,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {}
