package backend.example.civicbuild.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Resolves the client IP for rate-limiting keys.
 *
 * <p>Prefers the first hop of {@code X-Forwarded-For} when present (typical behind a load balancer),
 * falling back to the socket address. NOTE: {@code X-Forwarded-For} is client-spoofable unless a
 * trusted proxy overwrites it; in production this must only be trusted when the app sits behind a
 * proxy that sets it. Documented as a known trade-off for the current single-tier setup.
 */
public final class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
