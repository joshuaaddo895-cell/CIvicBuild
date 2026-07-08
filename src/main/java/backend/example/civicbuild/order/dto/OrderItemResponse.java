package backend.example.civicbuild.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String productName,
        String supplierName,
        BigDecimal unitPrice,
        BigDecimal quantity,
        String unit,
        BigDecimal lineTotal) {}
