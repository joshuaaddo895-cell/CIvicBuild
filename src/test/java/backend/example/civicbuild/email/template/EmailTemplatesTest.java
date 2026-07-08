package backend.example.civicbuild.email.template;

import static org.assertj.core.api.Assertions.assertThat;

import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderItem;
import backend.example.civicbuild.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailTemplatesTest {

    @Test
    void passwordChangedTemplate_rendersSafely() {
        String html = EmailTemplates.passwordChanged("Jane Doe");

        assertThat(html).contains("Password changed");
        assertThat(html).contains("Jane Doe");
    }

    @Test
    void accountDeletedTemplate_rendersSafely() {
        String html = EmailTemplates.accountDeleted("Jane Doe");

        assertThat(html).contains("Account deleted");
        assertThat(html).contains("Jane Doe");
    }

    @Test
    void paymentConfirmation_includesOrderItemsAndTotal() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(OrderStatus.PAID)
                .subtotal(new BigDecimal("200.00"))
                .total(new BigDecimal("200.00"))
                .currency("GHS")
                .deliveryAddress("12 Market Road")
                .deliveryCity("Accra")
                .deliveryRegion("Greater Accra")
                .phoneNumber("+233201234567")
                .paystackReference("CB-test-ref")
                .items(List.of(OrderItem.builder()
                        .productName("Cement")
                        .supplierName("BuildCo")
                        .unitPrice(new BigDecimal("100.00"))
                        .quantity(new BigDecimal("2"))
                        .unit("bags")
                        .lineTotal(new BigDecimal("200.00"))
                        .build()))
                .build();

        String html = EmailTemplates.paymentConfirmation("Jane Doe", order);

        assertThat(html).contains("Payment Received");
        assertThat(html).contains("GHS");
        assertThat(html).contains("200.00");
        assertThat(html).contains("Cement");
        assertThat(html).contains("CB-test-ref");
        assertThat(html).contains("12 Market Road");
    }
}
