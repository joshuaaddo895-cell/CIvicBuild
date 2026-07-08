package backend.example.civicbuild.order.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CheckoutException extends ApiException {

    public CheckoutException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
