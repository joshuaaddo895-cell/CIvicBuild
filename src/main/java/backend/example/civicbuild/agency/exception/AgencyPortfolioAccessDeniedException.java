package backend.example.civicbuild.agency.exception;

import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AgencyPortfolioAccessDeniedException extends ApiException {

    public AgencyPortfolioAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Only construction agencies can upload portfolio images");
    }
}
