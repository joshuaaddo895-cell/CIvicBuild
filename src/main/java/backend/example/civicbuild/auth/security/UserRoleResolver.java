package backend.example.civicbuild.auth.security;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the user's current role from the database. JWT access tokens embed role at
 * issuance time; after onboarding updates the role, the token may be stale until refresh.
 */
@Service
public class UserRoleResolver {

    private final UserRepository userRepository;

    public UserRoleResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Role resolveRole(AuthenticatedUser actor) {
        return userRepository
                .findById(actor.id())
                .map(User::getRole)
                .orElse(actor.role());
    }

    @Transactional(readOnly = true)
    public boolean hasAnyRole(AuthenticatedUser actor, Role... roles) {
        Role effective = resolveRole(actor);
        for (Role role : roles) {
            if (effective == role) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Role resolveRole(UUID userId, Role fallback) {
        return userRepository.findById(userId).map(User::getRole).orElse(fallback);
    }
}
