package backend.example.civicbuild.delivery.repository;

import backend.example.civicbuild.delivery.entity.DeliveryProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryProviderRepository extends JpaRepository<DeliveryProvider, UUID> {

    Optional<DeliveryProvider> findByUserId(UUID userId);

    List<DeliveryProvider> findByConstructionAgencyIdOrderBySubmittedAtDesc(UUID agencyId);
}
