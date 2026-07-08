package backend.example.civicbuild.payment.service;

import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderStatus;
import backend.example.civicbuild.order.repository.OrderRepository;
import backend.example.civicbuild.order.service.OrderStateMachine;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.email.EmailRecipient;
import backend.example.civicbuild.email.service.EmailPublisher;
import backend.example.civicbuild.payment.exception.PaymentAmountMismatchException;
import backend.example.civicbuild.payment.util.PaystackMoneyConverter;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final UserRepository userRepository;
    private final EmailPublisher emailPublisher;

    public PaymentReconciliationService(
            OrderRepository orderRepository,
            OrderStateMachine stateMachine,
            UserRepository userRepository,
            EmailPublisher emailPublisher) {
        this.orderRepository = orderRepository;
        this.stateMachine = stateMachine;
        this.userRepository = userRepository;
        this.emailPublisher = emailPublisher;
    }

    @Transactional
    public void reconcileSuccessfulPayment(String reference, long amountPesewas, String source, boolean strict) {
        Optional<Order> orderOpt = orderRepository.findByPaystackReference(reference);
        if (orderOpt.isEmpty()) {
            log.warn("Payment success for unknown reference {} via {}", reference, source);
            if (strict) {
                throw new PaymentAmountMismatchException();
            }
            return;
        }

        Order order = orderOpt.get();
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} already PAID; ignoring duplicate success from {}", order.getId(), source);
            return;
        }

        long expectedPesewas = PaystackMoneyConverter.toPesewas(order.getTotal());
        if (amountPesewas != expectedPesewas) {
            log.error(
                    "Amount mismatch for order {} reference {}: expected {} pesewas, got {} via {}",
                    order.getId(),
                    reference,
                    expectedPesewas,
                    amountPesewas,
                    source);
            if (order.getStatus() == OrderStatus.PENDING) {
                stateMachine.transition(order, OrderStatus.PROCESSING);
            }
            stateMachine.transition(order, OrderStatus.FAILED);
            orderRepository.save(order);
            if (strict) {
                throw new PaymentAmountMismatchException();
            }
            return;
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            stateMachine.transition(order, OrderStatus.PROCESSING);
        }
        stateMachine.transition(order, OrderStatus.PAID);
        orderRepository.save(order);
        log.info("Order {} marked PAID via {}", order.getId(), source);
        sendPaymentConfirmationEmail(order.getId());
    }

    private void sendPaymentConfirmationEmail(UUID orderId) {
        orderRepository.findByIdWithItems(orderId).ifPresentOrElse(order -> {
            userRepository.findById(order.getUserId()).ifPresentOrElse(
                    user -> emailPublisher.paymentConfirmation(EmailRecipient.from(user), order),
                    () -> log.warn(
                            "Skipping payment email — user {} not found for order {}",
                            order.getUserId(),
                            order.getId()));
        }, () -> log.warn("Skipping payment email — order {} not found", orderId));
    }

    @Transactional
    public void reconcileFailedPayment(String reference, String source) {
        orderRepository.findByPaystackReference(reference).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.REFUNDED) {
                log.warn("Ignoring failure signal for terminal order {} via {}", order.getId(), source);
                return;
            }
            if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PROCESSING) {
                stateMachine.transition(order, OrderStatus.FAILED);
                orderRepository.save(order);
                log.info("Order {} marked FAILED via {}", order.getId(), source);
            }
        });
    }
}
