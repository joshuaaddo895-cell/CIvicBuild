package backend.example.civicbuild.auth.security;

import backend.example.civicbuild.auth.entity.Role;
import java.util.UUID;

/**
 * Immutable principal placed in the security context for authenticated requests.
 * Carries just enough identity to authorize downstream calls without another DB hit.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {}
