package backend.example.civicbuild.catalog.dto;

import backend.example.civicbuild.catalog.entity.Supplier;
import java.math.BigDecimal;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String logoUrl,
        BigDecimal rating,
        int reviewCount,
        BigDecimal distanceKm,
        boolean verified,
        String categoryId) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getLogoUrl(),
                supplier.getRating(),
                supplier.getReviewCount(),
                supplier.getDistanceKm(),
                supplier.isVerified(),
                supplier.getCategory() != null ? supplier.getCategory().getId() : null);
    }
}
