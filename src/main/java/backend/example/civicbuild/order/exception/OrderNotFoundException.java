package backend.example.civicbuild.order.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Order not found");
    }
}
