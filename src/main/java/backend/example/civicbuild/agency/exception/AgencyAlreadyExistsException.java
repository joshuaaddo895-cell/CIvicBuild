package backend.example.civicbuild.agency.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AgencyAlreadyExistsException extends ApiException {
    public AgencyAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "You already have an agency profile");
    }
}
