package backend.example.civicbuild.verification.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.verification.dto.DocumentUploadResponse;
import backend.example.civicbuild.verification.dto.DocumentUrlResponse;
import backend.example.civicbuild.verification.entity.VerificationDocumentType;
import backend.example.civicbuild.verification.service.VerificationDocumentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationDocumentService verificationDocumentService;

    public VerificationController(VerificationDocumentService verificationDocumentService) {
        this.verificationDocumentService = verificationDocumentService;
    }

    @PostMapping("/upload-document")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file,
            @RequestParam("documentType") VerificationDocumentType documentType) {
        DocumentUploadResponse response =
                verificationDocumentService.uploadDocument(user, file, documentType);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded successfully", response));
    }

    @GetMapping("/{userId}/document-url")
    public ResponseEntity<ApiResponse<DocumentUrlResponse>> getDocumentUrl(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID userId,
            @RequestParam("documentType") VerificationDocumentType documentType) {
        DocumentUrlResponse response =
                verificationDocumentService.getSignedDocumentUrl(user, userId, documentType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
