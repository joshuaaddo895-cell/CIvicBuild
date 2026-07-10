package backend.example.civicbuild.storage;

import java.time.Duration;

/**
 * Abstraction over object storage. Business logic depends on this interface — not the Cloudinary SDK.
 */
public interface StorageService {

    StoredFile uploadPrivateDocument(byte[] data, String publicId, DetectedFileType fileType);

    StoredFile uploadPublicImage(byte[] data, String publicId, DetectedFileType fileType);

    String generateSignedPrivateUrl(String publicId, String resourceType, String format, Duration ttl);

    String generatePublicDeliveryUrl(String publicId, String resourceType);
}
