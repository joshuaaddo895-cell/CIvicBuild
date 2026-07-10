package backend.example.civicbuild.delivery.repository;

import backend.example.civicbuild.delivery.entity.DeliveryJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryJobRepository extends JpaRepository<DeliveryJob, UUID> {

    List<DeliveryJob> findByDeliveryProviderIdOrderByAssignedAtDesc(UUID providerId);

    Optional<DeliveryJob> findByIdAndDeliveryProviderId(UUID id, UUID providerId);
}
