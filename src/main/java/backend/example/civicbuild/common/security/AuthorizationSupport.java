package backend.example.civicbuild.common.security;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class AuthorizationSupport {

    private AuthorizationSupport() {}

    public static void requireRole(AuthenticatedUser user, Role... roles) {
        for (Role role : roles) {
            if (user.role() == role) {
                return;
            }
        }
        throw new AccessDeniedException();
    }

    public static void requireSelfOrAdmin(AuthenticatedUser user, java.util.UUID resourceUserId) {
        if (user.role() == Role.ADMIN || user.id().equals(resourceUserId)) {
            return;
        }
        throw new AccessDeniedException();
    }

    public static class AccessDeniedException extends ApiException {
        public AccessDeniedException() {
            super(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
    }
}
