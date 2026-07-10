package backend.example.civicbuild.admin.service;

import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.repository.AgencyRepository;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.entity.VerificationStatus;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.exception.ForbiddenException;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.notification.entity.NotificationType;
import backend.example.civicbuild.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminVerificationService {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final NotificationService notificationService;

    public AdminVerificationService(
            UserRepository userRepository,
            AgencyRepository agencyRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPending(AuthenticatedUser actor) {
        requireAdmin(actor);
        return userRepository.findAll().stream()
                .filter(u -> u.getVerificationStatus() == VerificationStatus.PENDING)
                .map(this::toPendingItem)
                .toList();
    }

    @Transactional
    public void approve(AuthenticatedUser actor, UUID userId) {
        requireAdmin(actor);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        userRepository.save(user);
        agencyRepository.findByOwnerId(userId).ifPresent(agency -> {
            agency.setVerified(true);
            agencyRepository.save(agency);
        });
        notificationService.notify(
                user,
                NotificationType.verification,
                "Verification approved",
                "Your account has been verified.",
                Map.of("userId", userId.toString()));
    }

    @Transactional
    public void reject(AuthenticatedUser actor, UUID userId) {
        requireAdmin(actor);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setVerificationStatus(VerificationStatus.REJECTED);
        userRepository.save(user);
        notificationService.notify(
                user,
                NotificationType.verification,
                "Verification rejected",
                "Your verification was rejected. Please re-submit documents.",
                Map.of("userId", userId.toString()));
    }

    private void requireAdmin(AuthenticatedUser actor) {
        if (actor.role() != Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
    }

    private Map<String, Object> toPendingItem(User user) {
        UUID agencyId = agencyRepository.findByOwnerId(user.getId()).map(Agency::getId).orElse(null);
        return Map.of(
                "userId", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "agencyId", agencyId != null ? agencyId : "");
    }
}
