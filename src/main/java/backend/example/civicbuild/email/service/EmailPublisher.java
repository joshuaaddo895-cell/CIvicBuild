package backend.example.civicbuild.email.service;

import backend.example.civicbuild.email.EmailRecipient;
import backend.example.civicbuild.order.entity.Order;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Schedules outbound email after the current database transaction commits, then dispatches the
 * actual send on {@code emailExecutor}. This avoids racing commits, detached entities, and
 * interface-proxy pitfalls with {@code @Async}.
 */
@Service
public class EmailPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailPublisher.class);

    private final EmailService emailService;
    private final Executor emailExecutor;

    public EmailPublisher(EmailService emailService, @Qualifier("emailExecutor") Executor emailExecutor) {
        this.emailService = emailService;
        this.emailExecutor = emailExecutor;
    }

    public void welcome(EmailRecipient recipient) {
        publish(recipient, () -> emailService.sendWelcomeEmail(recipient), "welcome");
    }

    public void passwordReset(EmailRecipient recipient, String resetLink) {
        publish(recipient, () -> emailService.sendPasswordResetEmail(recipient, resetLink), "password-reset");
    }

    public void passwordChanged(EmailRecipient recipient) {
        publish(recipient, () -> emailService.sendPasswordChangedEmail(recipient), "password-changed");
    }

    public void accountDeleted(EmailRecipient recipient) {
        publish(recipient, () -> emailService.sendAccountDeletedEmail(recipient), "account-deleted");
    }

    public void paymentConfirmation(EmailRecipient recipient, Order order) {
        if (recipient == null || order == null) {
            log.warn("Skipping payment-confirmation email — missing recipient or order");
            return;
        }
        publish(recipient, () -> emailService.sendPaymentConfirmationEmail(recipient, order), "payment-confirmation");
    }

    private void publish(EmailRecipient recipient, Runnable sendTask, String type) {
        if (recipient == null) {
            log.warn("Skipping {} email — user has no email address", type);
            return;
        }
        Runnable task = () -> {
            try {
                sendTask.run();
            } catch (RuntimeException ex) {
                log.error("Unexpected failure while sending {} email to {}", type, maskEmail(recipient.email()), ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailExecutor.execute(task);
                }
            });
        } else {
            emailExecutor.execute(task);
        }
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
