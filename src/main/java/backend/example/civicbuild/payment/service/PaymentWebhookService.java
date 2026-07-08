package backend.example.civicbuild.payment.service;

import backend.example.civicbuild.payment.entity.PaymentEvent;
import backend.example.civicbuild.payment.exception.InvalidPaystackSignatureException;
import backend.example.civicbuild.payment.repository.PaymentEventRepository;
import backend.example.civicbuild.payment.security.PaystackSignatureVerifier;
import backend.example.civicbuild.payment.security.PaystackWebhookIpVerifier;
import backend.example.civicbuild.payment.util.PaymentEventKeyDeriver;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaystackSignatureVerifier signatureVerifier;
    private final PaystackWebhookIpVerifier ipVerifier;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentEventKeyDeriver eventKeyDeriver;
    private final PaymentReconciliationService reconciliationService;
    private final Clock clock;

    public PaymentWebhookService(
            PaystackSignatureVerifier signatureVerifier,
            PaystackWebhookIpVerifier ipVerifier,
            PaymentEventRepository paymentEventRepository,
            PaymentEventKeyDeriver eventKeyDeriver,
            PaymentReconciliationService reconciliationService,
            Clock clock) {
        this.signatureVerifier = signatureVerifier;
        this.ipVerifier = ipVerifier;
        this.paymentEventRepository = paymentEventRepository;
        this.eventKeyDeriver = eventKeyDeriver;
        this.reconciliationService = reconciliationService;
        this.clock = clock;
    }

    @Transactional
    public void handleWebhook(byte[] rawBody, String signatureHeader, String clientIp) {
        if (!ipVerifier.isAllowed(clientIp)) {
            log.warn("Rejected Paystack webhook from disallowed IP");
            throw new InvalidPaystackSignatureException();
        }

        if (!signatureVerifier.isValid(rawBody, signatureHeader)) {
            log.warn("Rejected Paystack webhook with invalid signature from IP {}", clientIp);
            throw new InvalidPaystackSignatureException();
        }

        String rawPayload = new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);
        String eventKey = eventKeyDeriver.derive(rawPayload);

        if (paymentEventRepository.existsByEventKey(eventKey)) {
            log.info("Ignoring duplicate Paystack webhook event {}", eventKey);
            return;
        }

        PaymentEvent event = PaymentEvent.builder()
                .eventKey(eventKey)
                .eventType(eventKeyDeriver.eventType(rawPayload))
                .rawPayload(rawPayload)
                .build();
        paymentEventRepository.saveAndFlush(event);

        processEvent(rawPayload, event);
    }

    private void processEvent(String rawPayload, PaymentEvent event) {
        String eventType = event.getEventType();
        String reference = eventKeyDeriver.reference(rawPayload);

        try {
            if ("charge.success".equals(eventType)) {
                long amount = eventKeyDeriver.amountPesewas(rawPayload);
                reconciliationService.reconcileSuccessfulPayment(reference, amount, "webhook", false);
            } else if ("charge.failed".equals(eventType)) {
                reconciliationService.reconcileFailedPayment(reference, "webhook");
            }
            event.setProcessedAt(clock.instant());
            paymentEventRepository.save(event);
        } catch (RuntimeException e) {
            log.error("Failed processing Paystack webhook event {}", event.getEventKey(), e);
            throw e;
        }
    }
}
