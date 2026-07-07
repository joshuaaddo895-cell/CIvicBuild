package backend.example.civicbuild.email.service;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.email.template.EmailTemplates;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * {@link EmailService} backed by the Resend API. All sends run on the dedicated {@code emailExecutor}
 * and swallow provider failures (logged with context) so a mail outage never breaks registration or
 * password-reset flows.
 */
@Service
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final Resend resend;
    private final String from;

    public ResendEmailService(AppProperties properties) {
        this.resend = new Resend(properties.email().resendApiKey());
        this.from = properties.email().from();
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(User user) {
        send(user.getEmail(), EmailTemplates.WELCOME_SUBJECT,
                EmailTemplates.welcome(user.getFullName()), "welcome");
    }

    @Override
    @Async("emailExecutor")
    public void sendPasswordResetEmail(User user, String resetLink) {
        send(user.getEmail(), EmailTemplates.PASSWORD_RESET_SUBJECT,
                EmailTemplates.passwordReset(user.getFullName(), resetLink), "password-reset");
    }

    private void send(String to, String subject, String html, String type) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(html)
                .build();
        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.info("Sent {} email (id={})", type, response.getId());
        } catch (ResendException e) {
            // Never rethrow: email delivery is best-effort relative to the triggering operation.
            log.error("Failed to send {} email to recipient", type, e);
        }
    }
}
