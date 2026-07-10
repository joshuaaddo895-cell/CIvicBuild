package backend.example.civicbuild.notification.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.notification.entity.Notification;
import backend.example.civicbuild.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        List<NotificationResponse> items = notificationService.listForUser(user.id()).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        notificationService.markRead(user.id(), id);
        return ResponseEntity.ok(ApiResponse.message("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        notificationService.markAllRead(user.id());
        return ResponseEntity.ok(ApiResponse.message("All notifications marked as read"));
    }

    public record NotificationResponse(
            UUID id,
            String type,
            String title,
            String body,
            boolean read,
            Instant createdAt,
            Map<String, Object> data) {
        static NotificationResponse from(Notification notification) {
            return new NotificationResponse(
                    notification.getId(),
                    notification.getType().name(),
                    notification.getTitle(),
                    notification.getBody(),
                    notification.isRead(),
                    notification.getCreatedAt(),
                    notification.getData());
        }
    }
}
