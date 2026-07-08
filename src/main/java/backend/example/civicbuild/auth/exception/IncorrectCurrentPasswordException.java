package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a logged-in user submits the wrong current password during change-password. */
public class IncorrectCurrentPasswordException extends ApiException {

    public IncorrectCurrentPasswordException() {
        super(HttpStatus.FORBIDDEN, "Current password is incorrect");
    }
}
