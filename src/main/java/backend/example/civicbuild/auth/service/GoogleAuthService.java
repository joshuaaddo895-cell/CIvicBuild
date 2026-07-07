package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.dto.AuthResponse;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.entity.VerificationStatus;
import backend.example.civicbuild.auth.exception.AccountInactiveException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.GoogleTokenVerifierService;
import backend.example.civicbuild.auth.security.VerifiedGoogleProfile;
import backend.example.civicbuild.email.service.EmailService;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Google Sign-In use-case: verify the ID token, find-or-create by email, issue our own JWT pair.
 * Links to existing manual-signup accounts when the email matches — never creates duplicates.
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final GoogleTokenVerifierService googleTokenVerifier;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EmailService emailService;
    private final Clock clock;

    public GoogleAuthService(GoogleTokenVerifierService googleTokenVerifier,
            UserRepository userRepository, AuthService authService, EmailService emailService,
            Clock clock) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.authService = authService;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse signInWithGoogle(String rawIdToken) {
        VerifiedGoogleProfile profile = googleTokenVerifier.verify(rawIdToken);
        User user = userRepository.findByEmail(profile.email())
                .map(existing -> linkExistingUser(existing, profile))
                .orElseGet(() -> createGoogleUser(profile));
        return authService.issueTokensForUser(user);
    }

    private User linkExistingUser(User user, VerifiedGoogleProfile profile) {
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        applyGoogleProfile(user, profile);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(clock.instant());
        }
        log.info("Google Sign-In linked to existing user id={}", user.getId());
        return user;
    }

    private User createGoogleUser(VerifiedGoogleProfile profile) {
        Instant now = clock.instant();
        User user = User.builder()
                .fullName(profile.fullName())
                .email(profile.email())
                .passwordHash(null)
                .profilePictureUrl(profile.pictureUrl())
                .role(Role.CUSTOMER)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .active(true)
                .emailVerifiedAt(now)
                .build();
        User saved = userRepository.saveAndFlush(user);
        log.info("Created Google Sign-In user id={}", saved.getId());
        emailService.sendWelcomeEmail(saved);
        return saved;
    }

    private void applyGoogleProfile(User user, VerifiedGoogleProfile profile) {
        if (StringUtils.hasText(profile.fullName())) {
            user.setFullName(profile.fullName().trim());
        }
        if (StringUtils.hasText(profile.pictureUrl())) {
            user.setProfilePictureUrl(profile.pictureUrl());
        }
    }
}
