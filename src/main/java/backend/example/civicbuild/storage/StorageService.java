package backend.example.civicbuild.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Abstraction over object storage. Business logic depends on this interface — not the Cloudinary SDK.
 */
public interface StorageService {

    StoredFile uploadPrivateDocument(InputStream data, String publicId, DetectedFileType fileType);

    StoredFile uploadPublicImage(InputStream data, String publicId, DetectedFileType fileType);

    String generateSignedPrivateUrl(String publicId, String resourceType, String format, Duration ttl);
}
