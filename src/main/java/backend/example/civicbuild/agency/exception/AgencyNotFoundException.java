package backend.example.civicbuild.agency.exception;

import backend.example.civicbuild.common.exception.NotFoundException;

public class AgencyNotFoundException extends NotFoundException {
    public AgencyNotFoundException() {
        super("Agency not found");
    }
}
