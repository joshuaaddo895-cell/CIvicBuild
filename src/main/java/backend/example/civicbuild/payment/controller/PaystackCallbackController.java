package backend.example.civicbuild.payment.controller;

import backend.example.civicbuild.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paystack redirects the customer here after checkout. This is NOT a payment confirmation —
 * order status is updated only via verified webhook or server-side verify API.
 */
@RestController
@RequestMapping("/api/payments/paystack")
public class PaystackCallbackController {

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> callback() {
        return ResponseEntity.ok(ApiResponse.message(
                "Payment submitted. Return to the app to view your order status."));
    }
}
