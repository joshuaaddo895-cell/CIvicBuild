package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a Google ID token fails signature, audience, issuer, or expiry checks. */
public class InvalidGoogleTokenException extends ApiException {

    public InvalidGoogleTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired Google sign-in token");
    }
}
