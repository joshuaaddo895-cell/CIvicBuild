package backend.example.civicbuild.verification.service;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.storage.DetectedFileType;
import backend.example.civicbuild.storage.FileUploadValidator;
import backend.example.civicbuild.storage.StorageService;
import backend.example.civicbuild.storage.StoredFile;
import backend.example.civicbuild.storage.exception.InvalidFileUploadException;
import backend.example.civicbuild.verification.dto.DocumentUploadResponse;
import backend.example.civicbuild.verification.dto.DocumentUrlResponse;
import backend.example.civicbuild.verification.entity.VerificationDocument;
import backend.example.civicbuild.verification.entity.VerificationDocumentType;
import backend.example.civicbuild.verification.exception.DocumentAccessDeniedException;
import backend.example.civicbuild.verification.exception.VerificationDocumentNotFoundException;
import backend.example.civicbuild.verification.repository.VerificationDocumentRepository;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VerificationDocumentService {

    private static final Logger log = LoggerFactory.getLogger(VerificationDocumentService.class);
    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(5);

    private final VerificationDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileUploadValidator fileUploadValidator;
    private final Clock clock;

    public VerificationDocumentService(
            VerificationDocumentRepository documentRepository,
            UserRepository userRepository,
            StorageService storageService,
            FileUploadValidator fileUploadValidator,
            Clock clock) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileUploadValidator = fileUploadValidator;
        this.clock = clock;
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(
            AuthenticatedUser actor, MultipartFile file, VerificationDocumentType documentType) {
        fileUploadValidator.validateVerificationUpload(file);
        DetectedFileType fileType = fileUploadValidator.detectFileType(file);

        User user = userRepository
                .findById(actor.id())
                .orElseThrow(UserNotFoundException::new);
        assertVerificationRole(user.getRole());

        String publicId = fileUploadValidator.verificationPublicId(user.getId());
        StoredFile stored = uploadToCloudinary(file, publicId, fileType);

        VerificationDocument document = documentRepository
                .findByUserIdAndDocumentType(user.getId(), documentType)
                .map(existing -> replaceDocument(existing, stored))
                .orElseGet(() -> createDocument(user, documentType, stored));

        log.info(
                "Stored verification document id={} type={} for userId={}",
                document.getId(),
                documentType,
                user.getId());

        return new DocumentUploadResponse(
                document.getId(),
                document.getDocumentType(),
                document.getCloudinaryPublicId(),
                document.getResourceType());
    }

    @Transactional(readOnly = true)
    public DocumentUrlResponse getSignedDocumentUrl(
            AuthenticatedUser actor, UUID userId, VerificationDocumentType documentType) {
        assertCanViewDocuments(actor, userId);

        VerificationDocument document = documentRepository
                .findByUserIdAndDocumentType(userId, documentType)
                .orElseThrow(VerificationDocumentNotFoundException::new);

        String signedUrl = storageService.generateSignedPrivateUrl(
                document.getCloudinaryPublicId(),
                document.getResourceType(),
                document.getFormat(),
                SIGNED_URL_TTL);
        Instant expiresAt = clock.instant().plus(SIGNED_URL_TTL);
        return new DocumentUrlResponse(signedUrl, expiresAt);
    }

    private StoredFile uploadToCloudinary(MultipartFile file, String publicId, DetectedFileType fileType) {
        try {
            return storageService.uploadPrivateDocument(file.getInputStream(), publicId, fileType);
        } catch (IOException e) {
            throw new InvalidFileUploadException("Unable to read uploaded file");
        }
    }

    private VerificationDocument createDocument(
            User user, VerificationDocumentType documentType, StoredFile stored) {
        return documentRepository.save(VerificationDocument.builder()
                .user(user)
                .documentType(documentType)
                .cloudinaryPublicId(stored.publicId())
                .resourceType(stored.resourceType())
                .format(stored.format())
                .build());
    }

    private VerificationDocument replaceDocument(VerificationDocument existing, StoredFile stored) {
        return documentRepository.save(VerificationDocument.builder()
                .id(existing.getId())
                .user(existing.getUser())
                .documentType(existing.getDocumentType())
                .cloudinaryPublicId(stored.publicId())
                .resourceType(stored.resourceType())
                .format(stored.format())
                .createdAt(existing.getCreatedAt())
                .build());
    }

    private void assertVerificationRole(Role role) {
        if (role != Role.CONSTRUCTION_AGENCY && role != Role.DELIVERY_PROVIDER) {
            throw new DocumentAccessDeniedException();
        }
    }

    private void assertCanViewDocuments(AuthenticatedUser actor, UUID userId) {
        if (actor.role() == Role.ADMIN || actor.id().equals(userId)) {
            return;
        }
        throw new DocumentAccessDeniedException();
    }
}
