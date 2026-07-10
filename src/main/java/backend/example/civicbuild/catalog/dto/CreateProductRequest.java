package backend.example.civicbuild.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50) String category,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotBlank @Size(max = 50) String unit,
        @NotNull int stockQuantity,
        @Size(max = 512) String imageUrl,
        String description,
        @Size(max = 100) String brand,
        @Size(max = 255) String spec,
        @Size(max = 100) String deliveryEstimate) {}
