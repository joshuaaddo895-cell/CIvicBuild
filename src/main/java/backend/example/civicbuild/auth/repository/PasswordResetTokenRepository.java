package backend.example.civicbuild.auth.repository;

import backend.example.civicbuild.auth.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marks all of a user's outstanding (unused) reset tokens as used, so requesting a new reset
     * link invalidates any previously issued ones.
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken prt
               SET prt.usedAt = :now
             WHERE prt.user.id = :userId
               AND prt.usedAt IS NULL
            """)
    int invalidateOutstandingForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
