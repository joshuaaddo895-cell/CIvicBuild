package backend.example.civicbuild.saved.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.saved.entity.SavedSubjectType;
import backend.example.civicbuild.saved.service.SavedItemService;
import backend.example.civicbuild.saved.service.SavedItemService.SaveItemRequest;
import backend.example.civicbuild.saved.service.SavedItemService.SavedItemResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/saved")
public class SavedItemController {

    private final SavedItemService savedItemService;

    public SavedItemController(SavedItemService savedItemService) {
        this.savedItemService = savedItemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedItemResponse>>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(savedItemService.list(user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavedItemResponse>> save(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SaveItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Item saved", savedItemService.save(user, request)));
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable SavedSubjectType type,
            @PathVariable UUID id) {
        savedItemService.delete(user, type, id);
        return ResponseEntity.ok(ApiResponse.message("Item removed from saved"));
    }
}
