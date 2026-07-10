package backend.example.civicbuild.saved.repository;

import backend.example.civicbuild.saved.entity.SavedItem;
import backend.example.civicbuild.saved.entity.SavedSubjectType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedItemRepository extends JpaRepository<SavedItem, UUID> {

    List<SavedItem> findByUserIdOrderBySavedAtDesc(UUID userId);

    Optional<SavedItem> findByUserIdAndSubjectTypeAndSubjectId(
            UUID userId, SavedSubjectType subjectType, UUID subjectId);

    void deleteByUserIdAndSubjectTypeAndSubjectId(
            UUID userId, SavedSubjectType subjectType, UUID subjectId);
}
