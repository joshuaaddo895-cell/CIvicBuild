package backend.example.civicbuild.review.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.review.entity.ReviewSubjectType;
import backend.example.civicbuild.review.service.ReviewService;
import backend.example.civicbuild.review.service.ReviewService.CreateReviewRequest;
import backend.example.civicbuild.review.service.ReviewService.ReviewResponse;
import backend.example.civicbuild.review.service.ReviewService.ReviewSummaryResponse;
import backend.example.civicbuild.review.service.ReviewService.UpdateReviewRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> list(
            @RequestParam ReviewSubjectType subjectType, @RequestParam UUID subjectId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.list(subjectType, subjectId)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summary(
            @RequestParam ReviewSubjectType subjectType, @RequestParam UUID subjectId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.summary(subjectType, subjectId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> myReviews(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.myReviews(user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Review created", reviewService.create(user, request)));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Review updated", reviewService.update(user, reviewId, request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID reviewId) {
        reviewService.delete(user, reviewId);
        return ResponseEntity.ok(ApiResponse.message("Review deleted"));
    }
}
