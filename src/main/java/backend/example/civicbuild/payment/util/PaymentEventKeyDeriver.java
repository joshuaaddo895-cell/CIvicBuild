package backend.example.civicbuild.payment.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventKeyDeriver {

    private final ObjectMapper objectMapper;

    public PaymentEventKeyDeriver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String derive(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = textOrEmpty(root, "event");
            JsonNode data = root.path("data");
            if (data.hasNonNull("id")) {
                return event + ":" + data.get("id").asText();
            }
            String reference = textOrEmpty(data, "reference");
            String payloadHash = sha256(rawPayload);
            return event + ":" + reference + ":" + payloadHash;
        } catch (Exception e) {
            return "unknown:" + sha256(rawPayload);
        }
    }

    public String eventType(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            return textOrEmpty(root, "event");
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String reference(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            return textOrEmpty(root.path("data"), "reference");
        } catch (Exception e) {
            return "";
        }
    }

    public long amountPesewas(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            return root.path("data").path("amount").asLong(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
