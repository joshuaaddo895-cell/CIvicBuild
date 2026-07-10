package backend.example.civicbuild.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @Size(max = 255) String name,
        @Size(max = 50) String category,
        @DecimalMin("0.0") BigDecimal price,
        @Size(max = 50) String unit,
        Integer stockQuantity,
        @Size(max = 512) String imageUrl,
        String description,
        @Size(max = 100) String brand,
        @Size(max = 255) String spec,
        @Size(max = 100) String deliveryEstimate,
        Boolean active) {}
