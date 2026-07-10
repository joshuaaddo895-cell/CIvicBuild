package backend.example.civicbuild.delivery.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryProviderResponse(
        UUID id,
        UUID userId,
        String fullName,
        UUID constructionAgencyId,
        String vehicleInfo,
        String profileImageUrl,
        String approvalStatus,
        Instant submittedAt,
        Instant handledAt) {}
