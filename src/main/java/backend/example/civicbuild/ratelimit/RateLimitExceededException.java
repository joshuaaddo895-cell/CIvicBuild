package backend.example.civicbuild.ratelimit;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a caller exceeds the allowed number of attempts within the rate-limit window. */
public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please try again later.");
    }
}
