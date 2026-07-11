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
    void welcomeTemplate_rendersBrandedLayoutWithoutCta() {
        String html = EmailTemplates.welcome("Jane Doe");

        assertThat(html).contains("Welcome to CivicBuild, Jane Doe!");
        assertThat(html).contains("background-color:#4CAF50");
        assertThat(html).contains("Account created — you're almost ready to go");
        assertThat(html).doesNotContain("Sign In");
        assertThat(html).doesNotContain("<style>");
    }

    @Test
    void welcomeTemplate_escapesHtmlInName() {
        String html = EmailTemplates.welcome("<script>alert(1)</script>");

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void passwordChangedTemplate_rendersSafely() {
        String html = EmailTemplates.passwordChanged("Jane Doe");

        assertThat(html).contains("Your password was changed");
        assertThat(html).contains("Jane Doe");
        assertThat(html).contains("background-color:#4CAF50");
        assertThat(html).contains("Password updated - all sessions signed out");
    }

    @Test
    void accountDeletedTemplate_rendersSafely() {
        String html = EmailTemplates.accountDeleted("Jane Doe");

        assertThat(html).contains("Your account has been deleted");
        assertThat(html).contains("Jane Doe");
        assertThat(html).contains("Account deletion completed");
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

        assertThat(html).contains("Payment received");
        assertThat(html).contains("background-color:#4CAF50");
        assertThat(html).doesNotContain("linear-gradient");
        assertThat(html).contains("GHS");
        assertThat(html).contains("200.00");
        assertThat(html).contains("Cement");
        assertThat(html).contains("CB-test-ref");
        assertThat(html).contains("12 Market Road");
    }
}
