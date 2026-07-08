package backend.example.civicbuild.payment.security;

import backend.example.civicbuild.config.AppProperties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Optional defense-in-depth check against Paystack's published webhook source IPs.
 * Disabled by default in tests; enable in production via PAYSTACK_WEBHOOK_IP_CHECK=true.
 */
@Component
public class PaystackWebhookIpVerifier {

    private static final Logger log = LoggerFactory.getLogger(PaystackWebhookIpVerifier.class);

    private static final Set<String> PAYSTACK_WEBHOOK_IPS = Set.of(
            "52.31.139.75",
            "52.49.173.169",
            "52.214.14.220");

    private final boolean enabled;

    public PaystackWebhookIpVerifier(AppProperties properties) {
        this.enabled = properties.paystack().webhookIpCheckEnabled();
    }

    public boolean isAllowed(String clientIp) {
        if (!enabled) {
            return true;
        }
        if (clientIp == null || clientIp.isBlank()) {
            log.warn("Paystack webhook IP check enabled but client IP missing");
            return false;
        }
        boolean allowed = PAYSTACK_WEBHOOK_IPS.contains(clientIp.trim());
        if (!allowed) {
            log.warn("Paystack webhook received from non-allowlisted IP: {}", clientIp);
        }
        return allowed;
    }
}
