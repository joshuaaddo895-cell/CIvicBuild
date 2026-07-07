package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a refresh or password-reset token is missing, malformed, expired, revoked, or
 * already used. The message stays generic to avoid leaking which condition failed.
 */
public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
