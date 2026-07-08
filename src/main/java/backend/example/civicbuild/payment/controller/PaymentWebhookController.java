package backend.example.civicbuild.payment.controller;

import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.common.web.ClientIpResolver;
import backend.example.civicbuild.payment.exception.InvalidPaystackSignatureException;
import backend.example.civicbuild.payment.service.PaymentWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(HttpServletRequest request) throws IOException {
        byte[] rawBody = request.getInputStream().readAllBytes();
        String signature = request.getHeader("x-paystack-signature");
        String clientIp = ClientIpResolver.resolve(request);

        try {
            paymentWebhookService.handleWebhook(rawBody, signature, clientIp);
            return ResponseEntity.ok(ApiResponse.message("Webhook received"));
        } catch (InvalidPaystackSignatureException e) {
            throw e;
        } catch (RuntimeException e) {
            // Acknowledge receipt to avoid endless retries; event row exists for investigation.
            log.error("Webhook processing error after persistence", e);
            return ResponseEntity.ok(ApiResponse.message("Webhook received"));
        }
    }
}
