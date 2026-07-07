package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when registration is attempted with an email that already exists. */
public class DuplicateEmailException extends ApiException {

    public DuplicateEmailException() {
        super(HttpStatus.CONFLICT, "An account with this email already exists");
    }
}
