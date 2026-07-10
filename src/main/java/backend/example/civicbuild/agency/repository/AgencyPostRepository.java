package backend.example.civicbuild.agency.repository;

import backend.example.civicbuild.agency.entity.AgencyPost;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyPostRepository extends JpaRepository<AgencyPost, UUID> {

    Page<AgencyPost> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId, Pageable pageable);

    Optional<AgencyPost> findByIdAndAgencyId(UUID id, UUID agencyId);
}
