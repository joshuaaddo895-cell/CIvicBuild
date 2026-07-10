package backend.example.civicbuild.onboarding.dto;

import java.util.UUID;

public record DeliveryProviderProfileResponse(
        String fullName,
        UUID constructionAgencyId,
        String vehicleInfo,
        String profileImageUrl) {}
