package backend.example.civicbuild.order.service;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.order.dto.OrderItemResponse;
import backend.example.civicbuild.order.dto.OrderResponse;
import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderItem;
import backend.example.civicbuild.order.exception.OrderNotFoundException;
import backend.example.civicbuild.order.repository.OrderRepository;
import backend.example.civicbuild.payment.client.PaystackClient;
import backend.example.civicbuild.payment.client.PaystackVerifyResponse;
import backend.example.civicbuild.payment.service.PaymentReconciliationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaystackClient paystackClient;
    private final PaymentReconciliationService reconciliationService;

    public OrderService(
            OrderRepository orderRepository,
            PaystackClient paystackClient,
            PaymentReconciliationService reconciliationService) {
        this.orderRepository = orderRepository;
        this.paystackClient = paystackClient;
        this.reconciliationService = reconciliationService;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, AuthenticatedUser user) {
        Order order = orderRepository.findByIdAndUserId(orderId, user.id())
                .orElseThrow(OrderNotFoundException::new);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(AuthenticatedUser user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.id()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse verifyPayment(UUID orderId, AuthenticatedUser user) {
        Order order = orderRepository.findByIdAndUserId(orderId, user.id())
                .orElseThrow(OrderNotFoundException::new);

        if (order.getPaystackReference() == null) {
            throw new OrderNotFoundException();
        }

        PaystackVerifyResponse verify = paystackClient.verifyTransaction(order.getPaystackReference());
        if (verify.data() != null && "success".equalsIgnoreCase(verify.data().status())) {
            reconciliationService.reconcileSuccessfulPayment(
                    order.getPaystackReference(), verify.data().amount(), "verify", true);
        } else {
            reconciliationService.reconcileFailedPayment(order.getPaystackReference(), "verify");
        }

        Order refreshed = orderRepository.findByIdAndUserId(orderId, user.id())
                .orElseThrow(OrderNotFoundException::new);
        return toResponse(refreshed);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTotal(),
                order.getCurrency(),
                order.getDeliveryAddress(),
                order.getDeliveryCity(),
                order.getDeliveryRegion(),
                order.getPhoneNumber(),
                order.getPaystackReference(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getSupplierName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getUnit(),
                item.getLineTotal());
    }
}
