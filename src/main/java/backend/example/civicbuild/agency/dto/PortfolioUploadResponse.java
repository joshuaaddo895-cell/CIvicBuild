package backend.example.civicbuild.agency.dto;

import java.util.UUID;

public record PortfolioUploadResponse(
        UUID imageId, String publicId, String resourceType, String deliveryUrl) {}
