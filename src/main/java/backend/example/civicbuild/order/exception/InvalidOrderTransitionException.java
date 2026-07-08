package backend.example.civicbuild.order.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidOrderTransitionException extends ApiException {

    public InvalidOrderTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
