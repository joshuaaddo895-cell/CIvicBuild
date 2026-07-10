package backend.example.civicbuild.onboarding.repository;

import backend.example.civicbuild.onboarding.entity.UserOnboarding;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, UUID> {

    Optional<UserOnboarding> findByUserId(UUID userId);
}
