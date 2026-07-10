package backend.example.civicbuild.agency.exception;

import backend.example.civicbuild.common.exception.NotFoundException;

public class AgencyPostNotFoundException extends NotFoundException {
    public AgencyPostNotFoundException() {
        super("Post not found");
    }
}
