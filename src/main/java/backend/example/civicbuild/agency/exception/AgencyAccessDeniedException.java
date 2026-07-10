package backend.example.civicbuild.agency.exception;

import backend.example.civicbuild.common.exception.ForbiddenException;

public class AgencyAccessDeniedException extends ForbiddenException {
    public AgencyAccessDeniedException() {
        super("You do not have permission to manage this agency");
    }
}
