package backend.example.civicbuild.payment.repository;

import backend.example.civicbuild.payment.entity.PaymentEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    Optional<PaymentEvent> findByEventKey(String eventKey);

    boolean existsByEventKey(String eventKey);
}
