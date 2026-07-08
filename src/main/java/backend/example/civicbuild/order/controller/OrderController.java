package backend.example.civicbuild.order.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.common.web.ClientIpResolver;
import backend.example.civicbuild.order.dto.CheckoutRequest;
import backend.example.civicbuild.order.dto.CheckoutResponse;
import backend.example.civicbuild.order.dto.OrderResponse;
import backend.example.civicbuild.order.service.CheckoutService;
import backend.example.civicbuild.order.service.OrderService;
import backend.example.civicbuild.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final String ACTION_CHECKOUT = "checkout";

    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final RateLimiterService rateLimiter;

    public OrderController(
            CheckoutService checkoutService,
            OrderService orderService,
            RateLimiterService rateLimiter) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAndConsume(ACTION_CHECKOUT, ClientIpResolver.resolve(httpRequest), user.email());
        CheckoutResponse response = checkoutService.checkout(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<OrderResponse>> verify(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        OrderResponse order = orderService.verifyPayment(id, user);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrder(id, user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.listOrders(user)));
    }
}
