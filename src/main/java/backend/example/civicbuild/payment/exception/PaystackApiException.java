package backend.example.civicbuild.payment.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PaystackApiException extends ApiException {

    public PaystackApiException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
