package backend.example.civicbuild.order.service;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.order.dto.CheckoutRequest;
import backend.example.civicbuild.order.dto.CheckoutResponse;
import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderStatus;
import backend.example.civicbuild.order.repository.OrderRepository;
import backend.example.civicbuild.payment.client.PaystackClient;
import backend.example.civicbuild.payment.client.PaystackInitializeResponse;
import backend.example.civicbuild.payment.util.PaystackMoneyConverter;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private static final String REFERENCE_PREFIX = "CB-";

    private final OrderRepository orderRepository;
    private final OrderTotalCalculator totalCalculator;
    private final CheckoutItemResolver checkoutItemResolver;
    private final OrderStateMachine stateMachine;
    private final PaystackClient paystackClient;

    public CheckoutService(
            OrderRepository orderRepository,
            OrderTotalCalculator totalCalculator,
            CheckoutItemResolver checkoutItemResolver,
            OrderStateMachine stateMachine,
            PaystackClient paystackClient) {
        this.orderRepository = orderRepository;
        this.totalCalculator = totalCalculator;
        this.checkoutItemResolver = checkoutItemResolver;
        this.stateMachine = stateMachine;
        this.paystackClient = paystackClient;
    }

    @Transactional
    public CheckoutResponse checkout(AuthenticatedUser user, CheckoutRequest request) {
        var orderItems = request.items().stream().map(checkoutItemResolver::resolve).toList();
        BigDecimal subtotal = orderItems.stream()
                .map(item -> totalCalculator.lineTotal(item.getUnitPrice(), item.getQuantity()))
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal total = totalCalculator.total(subtotal);
        String reference = REFERENCE_PREFIX + UUID.randomUUID();

        Order order = Order.builder()
                .userId(user.id())
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .total(total)
                .currency("GHS")
                .deliveryAddress(request.delivery().address())
                .deliveryCity(request.delivery().city())
                .deliveryRegion(request.delivery().region())
                .phoneNumber(request.delivery().phoneNumber())
                .paystackReference(reference)
                .build();

        orderItems.forEach(order::addItem);

        orderRepository.saveAndFlush(order);

        try {
            long amountPesewas = PaystackMoneyConverter.toPesewas(total);
            PaystackInitializeResponse init = paystackClient.initializeTransaction(
                    user.email(), amountPesewas, reference, order.getCurrency());
            stateMachine.transition(order, OrderStatus.PROCESSING);
            orderRepository.save(order);
            return new CheckoutResponse(
                    order.getId(),
                    reference,
                    reference,
                    init.data().authorizationUrl(),
                    total);
        } catch (RuntimeException ex) {
            stateMachine.transition(order, OrderStatus.FAILED);
            orderRepository.save(order);
            throw ex;
        }
    }
}
