package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when an authenticated principal refers to a user row that no longer exists. */
public class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "User account not found");
    }
}
