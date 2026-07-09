package backend.example.civicbuild.verification.dto;

import backend.example.civicbuild.verification.entity.VerificationDocumentType;
import java.util.UUID;

public record DocumentUploadResponse(
        UUID documentId,
        VerificationDocumentType documentType,
        String publicId,
        String resourceType) {}
