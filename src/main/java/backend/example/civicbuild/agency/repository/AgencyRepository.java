package backend.example.civicbuild.agency.repository;

import backend.example.civicbuild.agency.entity.Agency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByOwnerId(UUID ownerId);

    @Query("SELECT a FROM Agency a WHERE (:q IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Agency> search(@Param("q") String q, Pageable pageable);
}
