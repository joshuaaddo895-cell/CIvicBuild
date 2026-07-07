package backend.example.civicbuild.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String ID_TOKEN = "mock-google-id-token";
    private static final VerifiedGoogleProfile PROFILE = new VerifiedGoogleProfile(
            "google.user@example.com", "Google User", "https://lh3.googleusercontent.com/photo.jpg");

    @Mock
    private GoogleTokenVerifierService googleTokenVerifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private EmailService emailService;

    private GoogleAuthService googleAuthService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-07T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        googleAuthService = new GoogleAuthService(
                googleTokenVerifier, userRepository, authService, emailService, clock);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(PROFILE);
    }

    @Test
    void signInWithGoogle_createsNewUserWithDefaults() {
        when(userRepository.findByEmail(PROFILE.email())).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(authService.issueTokensForUser(any(User.class)))
                .thenReturn(AuthResponse.bearer("access", "refresh", 900));

        AuthResponse response = googleAuthService.signInWithGoogle(ID_TOKEN);

        assertThat(response.accessToken()).isEqualTo("access");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo(PROFILE.email());
        assertThat(created.getFullName()).isEqualTo(PROFILE.fullName());
        assertThat(created.getProfilePictureUrl()).isEqualTo(PROFILE.pictureUrl());
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(created.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(created.getEmailVerifiedAt()).isEqualTo(clock.instant());
        verify(emailService).sendWelcomeEmail(any(User.class));
    }

    @Test
    void signInWithGoogle_linksExistingManualAccountByEmail() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .fullName("Manual Name")
                .email(PROFILE.email())
                .passwordHash("bcrypt-hash")
                .role(Role.CUSTOMER)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .active(true)
                .build();
        when(userRepository.findByEmail(PROFILE.email())).thenReturn(Optional.of(existing));
        when(authService.issueTokensForUser(existing))
                .thenReturn(AuthResponse.bearer("access", "refresh", 900));

        googleAuthService.signInWithGoogle(ID_TOKEN);

        assertThat(existing.getFullName()).isEqualTo(PROFILE.fullName());
        assertThat(existing.getProfilePictureUrl()).isEqualTo(PROFILE.pictureUrl());
        assertThat(existing.getEmailVerifiedAt()).isEqualTo(clock.instant());
        verify(userRepository, never()).saveAndFlush(any());
        verify(emailService, never()).sendWelcomeEmail(any());
    }

    @Test
    void signInWithGoogle_rejectsInactiveLinkedAccount() {
        User inactive = User.builder()
                .id(UUID.randomUUID())
                .email(PROFILE.email())
                .passwordHash("hash")
                .active(false)
                .build();
        when(userRepository.findByEmail(PROFILE.email())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> googleAuthService.signInWithGoogle(ID_TOKEN))
                .isInstanceOf(AccountInactiveException.class);
    }
}
