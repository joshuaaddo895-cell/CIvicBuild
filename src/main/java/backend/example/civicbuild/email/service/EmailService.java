package backend.example.civicbuild.email.service;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.order.entity.Order;

/**
 * Outbound transactional email. Implementations must be non-blocking / failure-tolerant from the
 * caller's perspective: a provider outage must never fail the business operation that triggered it.
 */
public interface EmailService {

    void sendWelcomeEmail(User user);

    void sendPasswordResetEmail(User user, String resetLink);

    void sendPaymentConfirmationEmail(User user, Order order);
}
