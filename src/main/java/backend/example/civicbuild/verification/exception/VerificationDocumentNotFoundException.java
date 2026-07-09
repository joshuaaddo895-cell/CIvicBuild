package backend.example.civicbuild.verification.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class VerificationDocumentNotFoundException extends ApiException {

    public VerificationDocumentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Verification document not found");
    }
}
