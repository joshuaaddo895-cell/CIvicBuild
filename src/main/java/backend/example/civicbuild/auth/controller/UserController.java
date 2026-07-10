package backend.example.civicbuild.auth.controller;

import backend.example.civicbuild.auth.dto.UpdateProfileRequest;
import backend.example.civicbuild.auth.dto.UserResponse;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.auth.service.AvatarService;
import backend.example.civicbuild.auth.service.UserProfileService;
import java.util.Map;
import backend.example.civicbuild.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final AvatarService avatarService;

    public UserController(UserProfileService userProfileService, AvatarService avatarService) {
        this.userProfileService = userProfileService;
        this.avatarService = avatarService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileService.getProfile(user)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse updated = userProfileService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded", avatarService.uploadAvatar(user, file)));
    }
}
