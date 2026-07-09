package backend.example.civicbuild.agency.repository;

import backend.example.civicbuild.agency.entity.AgencyPortfolioImage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyPortfolioImageRepository extends JpaRepository<AgencyPortfolioImage, UUID> {}
