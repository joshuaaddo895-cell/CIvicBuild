package backend.example.civicbuild.storage.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidFileUploadException extends ApiException {

    public InvalidFileUploadException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
