package backend.example.civicbuild.admin.controller;

import backend.example.civicbuild.admin.service.AdminVerificationService;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/verification")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    public AdminVerificationController(AdminVerificationService adminVerificationService) {
        this.adminVerificationService = adminVerificationService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listPending(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(adminVerificationService.listPending(user)));
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID userId) {
        adminVerificationService.approve(user, userId);
        return ResponseEntity.ok(ApiResponse.message("User verification approved"));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID userId) {
        adminVerificationService.reject(user, userId);
        return ResponseEntity.ok(ApiResponse.message("User verification rejected"));
    }
}
