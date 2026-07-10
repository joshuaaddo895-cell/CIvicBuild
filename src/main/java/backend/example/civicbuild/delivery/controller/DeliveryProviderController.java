package backend.example.civicbuild.delivery.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.delivery.dto.DeliveryJobResponse;
import backend.example.civicbuild.delivery.dto.DeliveryProviderResponse;
import backend.example.civicbuild.delivery.dto.DeliveryProviderSetupRequest;
import backend.example.civicbuild.delivery.service.DeliveryProviderService;
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
@RequestMapping("/api/delivery-providers")
public class DeliveryProviderController {

    private final DeliveryProviderService deliveryProviderService;

    public DeliveryProviderController(DeliveryProviderService deliveryProviderService) {
        this.deliveryProviderService = deliveryProviderService;
    }

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<DeliveryProviderResponse>> setup(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody DeliveryProviderSetupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Delivery profile created", deliveryProviderService.setup(user, request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DeliveryProviderResponse>> getMe(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryProviderService.getMe(user)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<DeliveryProviderResponse>> updateMe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody DeliveryProviderSetupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Delivery profile updated", deliveryProviderService.updateMe(user, request)));
    }

    @DeleteMapping("/me/association")
    public ResponseEntity<ApiResponse<Void>> removeAssociation(@AuthenticationPrincipal AuthenticatedUser user) {
        deliveryProviderService.removeAssociation(user);
        return ResponseEntity.ok(ApiResponse.message("Association removed"));
    }

    @GetMapping("/me/jobs")
    public ResponseEntity<ApiResponse<List<DeliveryJobResponse>>> listJobs(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryProviderService.listJobs(user)));
    }

    @PatchMapping("/me/jobs/{jobId}/status")
    public ResponseEntity<ApiResponse<DeliveryJobResponse>> updateJobStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID jobId,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Job updated", deliveryProviderService.updateJobStatus(user, jobId, status)));
    }
}
