package backend.example.civicbuild.order.service;

import backend.example.civicbuild.order.dto.CheckoutRequest;
import backend.example.civicbuild.order.entity.OrderItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderTotalCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal lineTotal(BigDecimal unitPrice, BigDecimal quantity) {
        return unitPrice.multiply(quantity).setScale(MONEY_SCALE, ROUNDING);
    }

    public BigDecimal subtotal(List<CheckoutRequest.CheckoutItemRequest> items) {
        return items.stream()
                .map(item -> lineTotal(item.unitPrice(), item.quantity()))
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b).setScale(MONEY_SCALE, ROUNDING));
    }

    public BigDecimal total(BigDecimal subtotal) {
        return subtotal.setScale(MONEY_SCALE, ROUNDING);
    }

    public OrderItem toOrderItem(CheckoutRequest.CheckoutItemRequest item) {
        BigDecimal lineTotal = lineTotal(item.unitPrice(), item.quantity());
        return OrderItem.builder()
                .productId(item.productId())
                .productName(item.productName())
                .supplierName(item.supplierName())
                .unitPrice(item.unitPrice().setScale(MONEY_SCALE, ROUNDING))
                .quantity(item.quantity().setScale(MONEY_SCALE, ROUNDING))
                .unit(item.unit())
                .lineTotal(lineTotal)
                .build();
    }
}
