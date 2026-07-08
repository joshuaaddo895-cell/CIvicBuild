package backend.example.civicbuild.email;

import backend.example.civicbuild.auth.entity.User;
import org.springframework.util.StringUtils;

/**
 * Immutable snapshot of user contact details for outbound email. Captured inside a transaction
 * so async delivery never depends on a live JPA entity.
 */
public record EmailRecipient(String email, String fullName) {

    public static EmailRecipient from(User user) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            return null;
        }
        String name = StringUtils.hasText(user.getFullName()) ? user.getFullName().trim() : "there";
        return new EmailRecipient(user.getEmail().trim(), name);
    }
}
