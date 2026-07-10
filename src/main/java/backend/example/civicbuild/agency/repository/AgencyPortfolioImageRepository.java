package backend.example.civicbuild.agency.repository;

import backend.example.civicbuild.agency.entity.AgencyPortfolioImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyPortfolioImageRepository extends JpaRepository<AgencyPortfolioImage, UUID> {

    List<AgencyPortfolioImage> findByUserIdOrderByCreatedAtDesc(UUID userId);

    java.util.Optional<AgencyPortfolioImage> findByIdAndUserId(UUID id, UUID userId);
}
