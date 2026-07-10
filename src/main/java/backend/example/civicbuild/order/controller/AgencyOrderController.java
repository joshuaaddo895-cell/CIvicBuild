package backend.example.civicbuild.order.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.order.entity.FulfillmentStatus;
import backend.example.civicbuild.order.service.AgencyOrderService;
import backend.example.civicbuild.order.service.AgencyOrderService.AgencyOrderResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agencies/me/orders")
public class AgencyOrderController {

    private final AgencyOrderService agencyOrderService;

    public AgencyOrderController(AgencyOrderService agencyOrderService) {
        this.agencyOrderService = agencyOrderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyOrderResponse>>> listOrders(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(agencyOrderService.listAgencyOrders(user)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AgencyOrderResponse>> getOrder(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.ok(agencyOrderService.getAgencyOrder(user, orderId)));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<AgencyOrderResponse>> updateStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID orderId,
            @RequestParam FulfillmentStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Order status updated", agencyOrderService.updateStatus(user, orderId, status)));
    }
}
