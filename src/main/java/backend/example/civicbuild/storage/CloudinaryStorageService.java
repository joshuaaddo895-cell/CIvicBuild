package backend.example.civicbuild.storage;

import backend.example.civicbuild.storage.exception.StorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageService.class);

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public StoredFile uploadPrivateDocument(byte[] data, String publicId, DetectedFileType fileType) {
        return upload(data, publicId, fileType, "authenticated", false);
    }

    @Override
    public StoredFile uploadPublicImage(byte[] data, String publicId, DetectedFileType fileType) {
        return upload(data, publicId, fileType, "upload", true);
    }

    @Override
    public String generateSignedPrivateUrl(
            String publicId, String resourceType, String format, Duration ttl) {
        try {
            long expiresAt = Instant.now().getEpochSecond() + ttl.getSeconds();
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", "authenticated",
                    "expires_at", expiresAt);
            return cloudinary.privateDownload(publicId, format, options);
        } catch (Exception e) {
            log.warn(
                    "Failed to generate signed Cloudinary URL for publicId={}: {}",
                    publicId,
                    e.getClass().getSimpleName());
            throw new StorageException("Unable to generate document access URL");
        }
    }

    @Override
    public String generatePublicDeliveryUrl(String publicId, String resourceType) {
        try {
            return cloudinary.url().resourceType(resourceType).secure(true).generate(publicId);
        } catch (Exception e) {
            log.warn("Failed to generate public URL for publicId={}", publicId);
            throw new StorageException("Unable to generate image URL");
        }
    }

    private StoredFile upload(
            byte[] data,
            String publicId,
            DetectedFileType fileType,
            String accessType,
            boolean includeDeliveryUrl) {
        if (data == null || data.length == 0) {
            throw new StorageException("File upload failed");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = cloudinary
                    .uploader()
                    .upload(
                            data,
                            ObjectUtils.asMap(
                                    "public_id", publicId,
                                    "type", accessType,
                                    "resource_type", fileType.cloudinaryResourceType(),
                                    "filename", filenameFor(fileType)));
            String storedPublicId = (String) response.get("public_id");
            String resourceType = (String) response.get("resource_type");
            String format = (String) response.get("format");
            String deliveryUrl = includeDeliveryUrl ? (String) response.get("secure_url") : null;
            return new StoredFile(storedPublicId, resourceType, format, deliveryUrl);
        } catch (IOException e) {
            log.warn("Cloudinary upload failed for publicId={}: {}", publicId, e.getMessage());
            throw new StorageException("File upload failed");
        } catch (Exception e) {
            log.warn(
                    "Cloudinary upload failed for publicId={}: {}",
                    publicId,
                    e.getClass().getSimpleName());
            throw new StorageException("File upload failed");
        }
    }

    private static String filenameFor(DetectedFileType fileType) {
        return switch (fileType) {
            case PDF -> "upload.pdf";
            case JPEG -> "upload.jpg";
            case PNG -> "upload.png";
        };
    }
}
