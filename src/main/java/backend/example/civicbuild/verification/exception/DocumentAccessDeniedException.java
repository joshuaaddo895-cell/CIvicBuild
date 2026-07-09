package backend.example.civicbuild.verification.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class DocumentAccessDeniedException extends ApiException {

    public DocumentAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "You are not allowed to access this document");
    }
}
