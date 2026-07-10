package backend.example.civicbuild.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @NotEmpty @Valid List<CheckoutItemRequest> items,
        @NotNull @Valid DeliveryDetailsRequest delivery) {

    public record CheckoutItemRequest(
            UUID productId,
            @NotBlank @Size(max = 255) String productName,
            @NotBlank @Size(max = 255) String supplierName,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice,
            @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
            @NotBlank @Size(max = 20) String unit) {}

    public record DeliveryDetailsRequest(
            @NotBlank @Size(max = 500) String address,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String region,
            @NotBlank @Size(max = 30) String phoneNumber) {}
}
