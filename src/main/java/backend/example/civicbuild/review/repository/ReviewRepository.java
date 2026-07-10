package backend.example.civicbuild.review.repository;

import backend.example.civicbuild.review.entity.Review;
import backend.example.civicbuild.review.entity.ReviewSubjectType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(
            ReviewSubjectType subjectType, UUID subjectId);

    List<Review> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Review> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.subjectType = :type AND r.subjectId = :id")
    Double averageRating(@Param("type") ReviewSubjectType type, @Param("id") UUID id);

    long countBySubjectTypeAndSubjectId(ReviewSubjectType subjectType, UUID subjectId);
}
