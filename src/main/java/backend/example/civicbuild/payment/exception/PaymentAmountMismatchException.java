package backend.example.civicbuild.payment.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PaymentAmountMismatchException extends ApiException {

    public PaymentAmountMismatchException() {
        super(HttpStatus.CONFLICT, "Payment amount does not match order total");
    }
}
