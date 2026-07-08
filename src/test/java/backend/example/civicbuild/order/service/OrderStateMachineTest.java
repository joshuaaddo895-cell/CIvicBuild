package backend.example.civicbuild.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderStatus;
import backend.example.civicbuild.order.exception.InvalidOrderTransitionException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;
    private Order order;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
        order = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("10.00"))
                .total(new BigDecimal("10.00"))
                .build();
    }

    @Test
    void pendingToProcessing_succeeds() {
        stateMachine.transition(order, OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void processingToPaid_succeeds() {
        order.setStatus(OrderStatus.PROCESSING);
        stateMachine.transition(order, OrderStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void paidToPending_rejected() {
        order.setStatus(OrderStatus.PAID);
        assertThatThrownBy(() -> stateMachine.transition(order, OrderStatus.PENDING))
                .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void failedIsTerminal() {
        order.setStatus(OrderStatus.FAILED);
        assertThatThrownBy(() -> stateMachine.transition(order, OrderStatus.PAID))
                .isInstanceOf(InvalidOrderTransitionException.class);
    }
}
