package backend.example.civicbuild.storage.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {

    public StorageException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
