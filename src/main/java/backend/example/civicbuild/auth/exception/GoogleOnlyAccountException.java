package backend.example.civicbuild.auth.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a Google-only account attempts manual email/password login. */
public class GoogleOnlyAccountException extends ApiException {

    public GoogleOnlyAccountException() {
        super(HttpStatus.BAD_REQUEST,
                "This account uses Google Sign-In. Please use Google to log in.");
    }
}
