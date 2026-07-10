package backend.example.civicbuild.review.service;

import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.review.entity.Review;
import backend.example.civicbuild.review.entity.ReviewSubjectType;
import backend.example.civicbuild.review.repository.ReviewRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(ReviewSubjectType subjectType, UUID subjectId) {
        return reviewRepository
                .findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(subjectType, subjectId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse summary(ReviewSubjectType subjectType, UUID subjectId) {
        Double avg = reviewRepository.averageRating(subjectType, subjectId);
        long total = reviewRepository.countBySubjectTypeAndSubjectId(subjectType, subjectId);
        return new ReviewSummaryResponse(avg != null ? avg : 0.0, total, List.of());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> myReviews(AuthenticatedUser actor) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(actor.id()).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse create(AuthenticatedUser actor, CreateReviewRequest request) {
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        Review review = reviewRepository.save(Review.builder()
                .subjectType(request.subjectType())
                .subjectId(request.subjectId())
                .user(user)
                .reviewerName(user.getFullName())
                .rating(request.rating())
                .text(request.text())
                .verifiedPurchase(request.verifiedPurchase())
                .orderNumber(request.orderNumber())
                .build());
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse update(AuthenticatedUser actor, UUID reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository
                .findByIdAndUserId(reviewId, actor.id())
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (request.rating() != null) review.setRating(request.rating());
        if (request.text() != null) review.setText(request.text());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public void delete(AuthenticatedUser actor, UUID reviewId) {
        Review review = reviewRepository
                .findByIdAndUserId(reviewId, actor.id())
                .orElseThrow(() -> new NotFoundException("Review not found"));
        reviewRepository.delete(review);
    }

    public record CreateReviewRequest(
            @NotNull ReviewSubjectType subjectType,
            @NotNull UUID subjectId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String text,
            boolean verifiedPurchase,
            @Size(max = 100) String orderNumber) {}

    public record UpdateReviewRequest(@Min(1) @Max(5) Integer rating, @Size(max = 2000) String text) {}

    public record ReviewResponse(
            UUID id,
            ReviewSubjectType subjectType,
            UUID subjectId,
            String reviewerName,
            int rating,
            String text,
            boolean verifiedPurchase,
            String orderNumber,
            Instant createdAt) {
        static ReviewResponse from(Review review) {
            return new ReviewResponse(
                    review.getId(),
                    review.getSubjectType(),
                    review.getSubjectId(),
                    review.getReviewerName(),
                    review.getRating(),
                    review.getText(),
                    review.isVerifiedPurchase(),
                    review.getOrderNumber(),
                    review.getCreatedAt());
        }
    }

    public record ReviewSummaryResponse(double averageRating, long totalCount, List<Map<String, Object>> breakdown) {}
}
