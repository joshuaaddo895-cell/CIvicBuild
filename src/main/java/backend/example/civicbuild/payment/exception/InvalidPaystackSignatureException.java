package backend.example.civicbuild.payment.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidPaystackSignatureException extends ApiException {

    public InvalidPaystackSignatureException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid Paystack webhook signature");
    }
}
