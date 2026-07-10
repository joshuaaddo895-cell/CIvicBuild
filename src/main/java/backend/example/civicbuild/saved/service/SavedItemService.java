package backend.example.civicbuild.saved.service;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.saved.entity.SavedItem;
import backend.example.civicbuild.saved.entity.SavedSubjectType;
import backend.example.civicbuild.saved.repository.SavedItemRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedItemService {

    private final SavedItemRepository savedItemRepository;
    private final UserRepository userRepository;

    public SavedItemService(SavedItemRepository savedItemRepository, UserRepository userRepository) {
        this.savedItemRepository = savedItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SavedItemResponse> list(AuthenticatedUser actor) {
        return savedItemRepository.findByUserIdOrderBySavedAtDesc(actor.id()).stream()
                .map(SavedItemResponse::from)
                .toList();
    }

    @Transactional
    public SavedItemResponse save(AuthenticatedUser actor, SaveItemRequest request) {
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        SavedItem item = savedItemRepository
                .findByUserIdAndSubjectTypeAndSubjectId(
                        user.getId(), request.type(), request.id())
                .orElseGet(() -> savedItemRepository.save(SavedItem.builder()
                        .user(user)
                        .subjectId(request.id())
                        .subjectType(request.type())
                        .build()));
        return SavedItemResponse.from(item);
    }

    @Transactional
    public void delete(AuthenticatedUser actor, SavedSubjectType type, UUID id) {
        savedItemRepository.deleteByUserIdAndSubjectTypeAndSubjectId(actor.id(), type, id);
    }

    public record SaveItemRequest(UUID id, SavedSubjectType type) {}

    public record SavedItemResponse(UUID id, SavedSubjectType type, java.time.Instant savedAt) {
        static SavedItemResponse from(SavedItem item) {
            return new SavedItemResponse(item.getSubjectId(), item.getSubjectType(), item.getSavedAt());
        }
    }
}
