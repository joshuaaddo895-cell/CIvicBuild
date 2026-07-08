package backend.example.civicbuild.email.service;

import backend.example.civicbuild.email.EmailRecipient;
import backend.example.civicbuild.order.entity.Order;

/**
 * Outbound transactional email. Implementations must be non-blocking / failure-tolerant from the
 * caller's perspective: a provider outage must never fail the business operation that triggered it.
 * Callers should use {@link EmailPublisher} so sends run after commit on the email executor.
 */
public interface EmailService {

    void sendWelcomeEmail(EmailRecipient recipient);

    void sendPasswordResetEmail(EmailRecipient recipient, String resetLink);

    void sendPasswordChangedEmail(EmailRecipient recipient);

    void sendAccountDeletedEmail(EmailRecipient recipient);

    void sendPaymentConfirmationEmail(EmailRecipient recipient, Order order);
}
