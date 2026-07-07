package backend.example.civicbuild.auth.security;

import backend.example.civicbuild.auth.entity.Role;
import java.util.UUID;

/** Validated claims extracted from an access token. */
public record AccessTokenClaims(UUID userId, String email, Role role) {}
