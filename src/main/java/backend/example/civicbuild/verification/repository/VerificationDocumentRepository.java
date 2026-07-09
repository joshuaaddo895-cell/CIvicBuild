package backend.example.civicbuild.verification.repository;

import backend.example.civicbuild.verification.entity.VerificationDocument;
import backend.example.civicbuild.verification.entity.VerificationDocumentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, UUID> {

    Optional<VerificationDocument> findByUserIdAndDocumentType(UUID userId, VerificationDocumentType documentType);
}
