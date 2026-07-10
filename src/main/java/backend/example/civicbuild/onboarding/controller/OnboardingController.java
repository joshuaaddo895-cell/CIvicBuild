package backend.example.civicbuild.onboarding.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.onboarding.dto.OnboardingResponse;
import backend.example.civicbuild.onboarding.dto.UpdateOnboardingRequest;
import backend.example.civicbuild.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> getOnboarding(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.getOnboarding(user)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> updateOnboarding(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateOnboardingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Onboarding updated", onboardingService.updateOnboarding(user, request)));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<OnboardingResponse>> completeOnboarding(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Onboarding completed", onboardingService.completeOnboarding(user)));
    }
}
