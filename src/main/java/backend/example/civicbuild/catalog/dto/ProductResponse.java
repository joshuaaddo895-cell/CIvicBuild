package backend.example.civicbuild.catalog.dto;

import backend.example.civicbuild.catalog.entity.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String category,
        BigDecimal price,
        String unit,
        String imageUrl,
        String description,
        UUID supplierId,
        UUID agencyId,
        int stockQuantity,
        boolean inStock,
        String brand,
        String spec,
        String deliveryEstimate) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getUnit(),
                product.getImageUrl(),
                product.getDescription(),
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                product.getAgency() != null ? product.getAgency().getId() : null,
                product.getStockQuantity(),
                product.getStockQuantity() > 0,
                product.getBrand(),
                product.getSpec(),
                product.getDeliveryEstimate());
    }
}
