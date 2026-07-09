package backend.example.civicbuild.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.example.civicbuild.storage.exception.InvalidFileUploadException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadValidatorTest {

    private FileUploadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileUploadValidator();
    }

    @Test
    void detectFileType_acceptsPdfJpgAndPng() {
        assertThat(validator.detectFileType(pdfFile("doc.pdf"))).isEqualTo(DetectedFileType.PDF);
        assertThat(validator.detectFileType(jpegFile("photo.jpg"))).isEqualTo(DetectedFileType.JPEG);
        assertThat(validator.detectFileType(pngFile("image.png"))).isEqualTo(DetectedFileType.PNG);
    }

    @Test
    void validateVerificationUpload_rejectsOversizedFile() {
        byte[] bytes = new byte[(int) FileUploadValidator.MAX_VERIFICATION_FILE_BYTES + 1];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", bytes);

        assertThatThrownBy(() -> validator.validateVerificationUpload(file))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void validateVerificationUpload_rejectsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> validator.validateVerificationUpload(file))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessageContaining("Only PDF, JPG, and PNG");
    }

    @Test
    void validateVerificationUpload_rejectsUnsafeFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "../evil.pdf", "application/pdf", pdfBytes());

        assertThatThrownBy(() -> validator.validateVerificationUpload(file))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    void verificationPublicId_isServerControlledAndUuidBased() {
        UUID userId = UUID.randomUUID();

        String first = validator.verificationPublicId(userId);
        String second = validator.verificationPublicId(userId);

        assertThat(first).startsWith("verification-docs/" + userId + "/");
        assertThat(second).startsWith("verification-docs/" + userId + "/");
        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("..");
    }

    @Test
    void portfolioPublicId_isServerControlledAndUuidBased() {
        UUID userId = UUID.randomUUID();

        String publicId = validator.portfolioPublicId(userId);

        assertThat(publicId).startsWith("agency-portfolio/" + userId + "/");
        assertThat(publicId).doesNotContain("..");
    }

    private MockMultipartFile pdfFile(String name) {
        return new MockMultipartFile("file", name, "application/pdf", pdfBytes());
    }

    private MockMultipartFile jpegFile(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", jpegBytes());
    }

    private MockMultipartFile pngFile(String name) {
        return new MockMultipartFile("file", name, "image/png", pngBytes());
    }

    private byte[] pdfBytes() {
        return "%PDF-1.4".getBytes();
    }

    private byte[] jpegBytes() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
    }

    private byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    }
}
