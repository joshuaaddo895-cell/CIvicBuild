package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown on failed login. The message is deliberately generic — it never reveals whether the
 * email or the password was the problem, to avoid user enumeration.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
