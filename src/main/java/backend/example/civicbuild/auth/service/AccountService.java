package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.email.EmailRecipient;
import backend.example.civicbuild.email.service.EmailPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lifecycle operations. User deletion relies on database ON DELETE CASCADE to remove
 * refresh tokens, password reset tokens, orders, order items, and linked payment events.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;

    private final EmailPublisher emailPublisher;

    public AccountService(UserRepository userRepository, EmailPublisher emailPublisher) {
        this.userRepository = userRepository;
        this.emailPublisher = emailPublisher;
    }

    @Transactional
    public void deleteAccount(AuthenticatedUser principal) {
        UUID userId = principal.id();
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        EmailRecipient recipient = EmailRecipient.from(user);
        emailPublisher.accountDeleted(recipient);
        userRepository.delete(user);
        log.info("Deleted user account id={}", userId);
    }
}
