package backend.example.civicbuild.messaging.repository;

import backend.example.civicbuild.messaging.entity.MessageThread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {

    Optional<MessageThread> findByCustomerIdAndAgencyId(UUID customerId, UUID agencyId);

    @Query("SELECT t FROM MessageThread t WHERE t.customer.id = :userId OR t.agency.owner.id = :userId ORDER BY t.updatedAt DESC")
    List<MessageThread> findForUser(@Param("userId") UUID userId);
}
