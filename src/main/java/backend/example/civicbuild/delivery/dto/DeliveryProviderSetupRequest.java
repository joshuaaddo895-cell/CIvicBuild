package backend.example.civicbuild.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DeliveryProviderSetupRequest(
        @NotBlank @Size(max = 150) String fullName,
        UUID constructionAgencyId,
        @Size(max = 300) String vehicleInfo,
        @Size(max = 512) String profileImageUrl) {}
