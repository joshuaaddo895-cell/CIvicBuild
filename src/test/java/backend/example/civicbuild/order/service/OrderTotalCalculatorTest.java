package backend.example.civicbuild.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.example.civicbuild.order.dto.CheckoutRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTotalCalculatorTest {

    private final OrderTotalCalculator calculator = new OrderTotalCalculator();

    @Test
    void calculatesFractionalQuantityTotals() {
        List<CheckoutRequest.CheckoutItemRequest> items = List.of(
                new CheckoutRequest.CheckoutItemRequest(
                        null, "Cement", "BuildCo", new BigDecimal("120.50"), new BigDecimal("2.5"), "tons"),
                new CheckoutRequest.CheckoutItemRequest(
                        null, "Sand", "SandCo", new BigDecimal("80.00"), new BigDecimal("1"), "m3"));

        BigDecimal subtotal = calculator.subtotal(items);
        assertThat(subtotal).isEqualByComparingTo("381.25");
        assertThat(calculator.total(subtotal)).isEqualByComparingTo("381.25");
    }
}
