package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a deactivated account attempts to authenticate. */
public class AccountInactiveException extends ApiException {

    public AccountInactiveException() {
        super(HttpStatus.FORBIDDEN, "This account has been deactivated. Please contact support.");
    }
}
