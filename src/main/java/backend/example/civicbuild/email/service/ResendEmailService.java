package backend.example.civicbuild.email.service;

import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.email.EmailRecipient;
import backend.example.civicbuild.email.template.EmailTemplates;
import backend.example.civicbuild.order.entity.Order;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * {@link EmailService} backed by the Resend API. Invoked from {@link EmailPublisher} on the
 * dedicated {@code emailExecutor}; provider failures are logged and swallowed.
 */
@Service
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final Resend resend;
    private final String from;

    public ResendEmailService(AppProperties properties) {
        String apiKey = properties.email().resendApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }
        this.resend = new Resend(apiKey);
        this.from = properties.email().from();
        log.info("Resend email service configured (from: {})", from);
        String resetBaseUrl = properties.email().resetBaseUrl();
        if (resetBaseUrl != null && resetBaseUrl.contains("localhost")) {
            log.warn(
                    "PASSWORD_RESET_BASE_URL is set to {} — forgot-password emails will contain a localhost link",
                    resetBaseUrl);
        }
    }

    @Override
    public void sendWelcomeEmail(EmailRecipient recipient) {
        send(recipient, EmailTemplates.WELCOME_SUBJECT, EmailTemplates.welcome(recipient.fullName()), "welcome");
    }

    @Override
    public void sendPasswordResetEmail(EmailRecipient recipient, String resetLink) {
        send(recipient, EmailTemplates.PASSWORD_RESET_SUBJECT,
                EmailTemplates.passwordReset(recipient.fullName(), resetLink), "password-reset");
    }

    @Override
    public void sendPasswordChangedEmail(EmailRecipient recipient) {
        send(recipient, EmailTemplates.PASSWORD_CHANGED_SUBJECT,
                EmailTemplates.passwordChanged(recipient.fullName()), "password-changed");
    }

    @Override
    public void sendAccountDeletedEmail(EmailRecipient recipient) {
        send(recipient, EmailTemplates.ACCOUNT_DELETED_SUBJECT,
                EmailTemplates.accountDeleted(recipient.fullName()), "account-deleted");
    }

    @Override
    public void sendPaymentConfirmationEmail(EmailRecipient recipient, Order order) {
        send(recipient, EmailTemplates.PAYMENT_CONFIRMATION_SUBJECT,
                EmailTemplates.paymentConfirmation(recipient.fullName(), order), "payment-confirmation");
    }

    private void send(EmailRecipient recipient, String subject, String html, String type) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(recipient.email())
                .subject(subject)
                .html(html)
                .build();
        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.info("Sent {} email to {} (id={})", type, maskEmail(recipient.email()), response.getId());
        } catch (ResendException e) {
            log.error("Failed to send {} email to {} — Resend error: {}", type, maskEmail(recipient.email()), e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Unexpected failure sending {} email to {}", type, maskEmail(recipient.email()), e);
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
