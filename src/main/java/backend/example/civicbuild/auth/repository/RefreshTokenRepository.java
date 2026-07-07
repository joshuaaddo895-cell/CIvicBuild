package backend.example.civicbuild.auth.repository;

import backend.example.civicbuild.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes every currently-active refresh token for a user. Used on logout-all and on password
     * reset to force re-login across all devices.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt
               SET rt.revokedAt = :now
             WHERE rt.user.id = :userId
               AND rt.revokedAt IS NULL
            """)
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
