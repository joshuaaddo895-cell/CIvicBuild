package backend.example.civicbuild.order.service;

import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.service.AgencyService;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.order.entity.FulfillmentStatus;
import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderItem;
import backend.example.civicbuild.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgencyOrderService {

    private final OrderRepository orderRepository;
    private final AgencyService agencyService;
    private final UserRepository userRepository;

    public AgencyOrderService(
            OrderRepository orderRepository, AgencyService agencyService, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.agencyService = agencyService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AgencyOrderResponse> listAgencyOrders(AuthenticatedUser actor) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        return orderRepository.findDistinctByAgencyId(agency.getId()).stream()
                .map(order -> toAgencyOrder(order, agency.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AgencyOrderResponse getAgencyOrder(AuthenticatedUser actor, UUID orderId) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        Order order = orderRepository
                .findByIdWithItems(orderId)
                .filter(o -> o.getItems().stream().anyMatch(i -> agency.getId().equals(i.getAgencyId())))
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return toAgencyOrder(order, agency.getId());
    }

    @Transactional
    public AgencyOrderResponse updateStatus(AuthenticatedUser actor, UUID orderId, FulfillmentStatus status) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        Order order = orderRepository
                .findByIdWithItems(orderId)
                .filter(o -> o.getItems().stream().anyMatch(i -> agency.getId().equals(i.getAgencyId())))
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setFulfillmentStatus(status);
        return toAgencyOrder(orderRepository.save(order), agency.getId());
    }

    private AgencyOrderResponse toAgencyOrder(Order order, UUID agencyId) {
        User customer = userRepository.findById(order.getUserId()).orElse(null);
        List<AgencyOrderItemResponse> items = order.getItems().stream()
                .filter(item -> agencyId.equals(item.getAgencyId()))
                .map(this::toItem)
                .toList();
        return new AgencyOrderResponse(
                order.getId(),
                order.getPaystackReference(),
                order.getUserId(),
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getEmail() : null,
                order.getPhoneNumber(),
                agencyId,
                order.getCreatedAt(),
                order.getFulfillmentStatus().name(),
                formatAddress(order),
                order.getTotal(),
                items);
    }

    private AgencyOrderItemResponse toItem(OrderItem item) {
        return new AgencyOrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnit());
    }

    private String formatAddress(Order order) {
        return order.getDeliveryAddress() + ", " + order.getDeliveryCity() + ", " + order.getDeliveryRegion();
    }

    public record AgencyOrderResponse(
            UUID id,
            String orderNumber,
            UUID customerId,
            String customerName,
            String customerEmail,
            String customerPhone,
            UUID agencyId,
            Instant orderDate,
            String status,
            String deliveryAddress,
            BigDecimal totalAmount,
            List<AgencyOrderItemResponse> items) {}

    public record AgencyOrderItemResponse(
            UUID productId,
            String productName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String unit) {}
}
