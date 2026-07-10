package backend.example.civicbuild.delivery.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryJobResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String pickupAddress,
        String deliveryAddress,
        String status,
        Instant assignedAt) {}
