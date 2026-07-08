package backend.example.civicbuild.payment.util;

import java.util.Locale;
import java.util.Set;

public final class PaystackTransactionStatus {

    private static final Set<String> SUCCESS = Set.of("success");
    private static final Set<String> PENDING = Set.of(
            "pending", "ongoing", "processing", "queued", "open_url", "send_otp", "send_pin", "send_phone", "send_birthday");

    private PaystackTransactionStatus() {
    }

    public static boolean isSuccess(String status) {
        return status != null && SUCCESS.contains(status.toLowerCase(Locale.ROOT));
    }

    public static boolean isPending(String status) {
        return status != null && PENDING.contains(status.toLowerCase(Locale.ROOT));
    }

    public static boolean isTerminalFailure(String status) {
        return status != null
                && !isSuccess(status)
                && !isPending(status);
    }
}
