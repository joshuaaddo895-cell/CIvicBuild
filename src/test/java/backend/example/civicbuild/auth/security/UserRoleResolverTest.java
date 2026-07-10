package backend.example.civicbuild.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRoleResolverTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRoleResolver userRoleResolver;

    @Test
    void resolveRole_returnsDatabaseRoleWhenJwtIsStale() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(userId, "user@example.com", Role.CUSTOMER);
        User user = User.builder().id(userId).role(Role.CONSTRUCTION_AGENCY).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(userRoleResolver.resolveRole(actor)).isEqualTo(Role.CONSTRUCTION_AGENCY);
        assertThat(userRoleResolver.hasAnyRole(actor, Role.CONSTRUCTION_AGENCY)).isTrue();
        assertThat(userRoleResolver.hasAnyRole(actor, Role.CUSTOMER)).isFalse();
    }
}
