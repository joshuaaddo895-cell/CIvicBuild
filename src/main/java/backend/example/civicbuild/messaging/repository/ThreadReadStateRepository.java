package backend.example.civicbuild.messaging.repository;

import backend.example.civicbuild.messaging.entity.ThreadReadState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadReadStateRepository extends JpaRepository<ThreadReadState, ThreadReadState.ThreadReadStateId> {

    Optional<ThreadReadState> findByThreadIdAndUserId(UUID threadId, UUID userId);
}
