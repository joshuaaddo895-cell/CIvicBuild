package backend.example.civicbuild.agency.dto;

import java.time.Instant;
import java.util.UUID;

public record PersonnelResponse(
        UUID id,
        UUID userId,
        String fullName,
        String profileImageUrl,
        UUID constructionAgencyId,
        String vehicleInfo,
        String approvalStatus,
        Instant submittedAt,
        Instant handledAt) {}
