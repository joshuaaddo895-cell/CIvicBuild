package backend.example.civicbuild.order.service;

import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderStatus;
import backend.example.civicbuild.order.exception.InvalidOrderTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderStateMachine {

    private static final Logger log = LoggerFactory.getLogger(OrderStateMachine.class);

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.FAILED));
        ALLOWED.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.PAID, OrderStatus.FAILED));
        ALLOWED.put(OrderStatus.PAID, EnumSet.of(OrderStatus.REFUNDED));
        ALLOWED.put(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public void transition(Order order, OrderStatus target) {
        OrderStatus current = order.getStatus();
        if (current == target) {
            return;
        }
        Set<OrderStatus> allowedTargets = ALLOWED.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowedTargets.contains(target)) {
            String message = "Invalid order transition from %s to %s for order %s"
                    .formatted(current, target, order.getId());
            log.warn(message);
            throw new InvalidOrderTransitionException(message);
        }
        order.setStatus(target);
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return true;
        }
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }
}
