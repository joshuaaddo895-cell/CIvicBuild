package backend.example.civicbuild.messaging.repository;

import backend.example.civicbuild.messaging.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByThreadIdOrderBySentAtAsc(UUID threadId);
}
