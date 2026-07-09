package backend.example.civicbuild.storage;

import backend.example.civicbuild.storage.exception.InvalidFileUploadException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUploadValidator {

    public static final long MAX_VERIFICATION_FILE_BYTES = 5L * 1024 * 1024;

    private static final byte[] PDF_MAGIC = "%PDF".getBytes();
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public void validateVerificationUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException("File is required");
        }
        if (file.getSize() > MAX_VERIFICATION_FILE_BYTES) {
            throw new InvalidFileUploadException("File exceeds the 5MB size limit");
        }
        rejectUnsafeFilename(file.getOriginalFilename());
        detectFileType(file);
    }

    public void validatePortfolioUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException("File is required");
        }
        if (file.getSize() > MAX_VERIFICATION_FILE_BYTES) {
            throw new InvalidFileUploadException("File exceeds the 5MB size limit");
        }
        rejectUnsafeFilename(file.getOriginalFilename());
        DetectedFileType type = detectFileType(file);
        if (type == DetectedFileType.PDF) {
            throw new InvalidFileUploadException("Portfolio images must be JPG or PNG");
        }
    }

    public DetectedFileType detectFileType(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            if (startsWith(header, PDF_MAGIC)) {
                return DetectedFileType.PDF;
            }
            if (startsWith(header, JPEG_MAGIC)) {
                return DetectedFileType.JPEG;
            }
            if (startsWith(header, PNG_MAGIC)) {
                return DetectedFileType.PNG;
            }
            throw new InvalidFileUploadException("Only PDF, JPG, and PNG files are allowed");
        } catch (IOException e) {
            throw new InvalidFileUploadException("Unable to read uploaded file");
        }
    }

    public String verificationPublicId(UUID userId) {
        return "verification-docs/" + userId + "/" + UUID.randomUUID();
    }

    public String portfolioPublicId(UUID userId) {
        return "agency-portfolio/" + userId + "/" + UUID.randomUUID();
    }

    private void rejectUnsafeFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return;
        }
        String normalized = originalFilename.replace('\\', '/');
        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\0")) {
            throw new InvalidFileUploadException("Invalid filename");
        }
    }

    private boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
