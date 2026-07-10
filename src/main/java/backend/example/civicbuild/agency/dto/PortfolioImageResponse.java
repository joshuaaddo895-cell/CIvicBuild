package backend.example.civicbuild.agency.dto;

import java.util.UUID;

public record PortfolioImageResponse(
        UUID imageId,
        String publicId,
        String resourceType,
        String deliveryUrl) {}
