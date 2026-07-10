package backend.example.civicbuild.notification.service;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.notification.entity.Notification;
import backend.example.civicbuild.notification.entity.NotificationType;
import backend.example.civicbuild.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User user, NotificationType type, String title, String body, Map<String, Object> data) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .data(data)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Notification> listForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Forbidden");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }
}
