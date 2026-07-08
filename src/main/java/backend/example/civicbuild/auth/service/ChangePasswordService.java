package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.dto.ChangePasswordRequest;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.GoogleOnlyAccountException;
import backend.example.civicbuild.auth.exception.IncorrectCurrentPasswordException;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.RefreshTokenRepository;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.email.EmailRecipient;
import backend.example.civicbuild.email.service.EmailPublisher;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final EmailPublisher emailPublisher;

    public ChangePasswordService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            EmailPublisher emailPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.emailPublisher = emailPublisher;
    }

    @Transactional
    public void changePassword(AuthenticatedUser principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(UserNotFoundException::new);

        if (user.getPasswordHash() == null) {
            throw new GoogleOnlyAccountException();
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        Instant now = clock.instant();
        int revoked = refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
        log.info("Password changed for user id={}; revoked {} active refresh token(s)", user.getId(), revoked);
        emailPublisher.passwordChanged(EmailRecipient.from(user));
    }
}
