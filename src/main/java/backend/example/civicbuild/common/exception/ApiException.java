package backend.example.civicbuild.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all expected, client-facing errors. Carries the HTTP status so the
 * central handler can translate it consistently without leaking internals.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
