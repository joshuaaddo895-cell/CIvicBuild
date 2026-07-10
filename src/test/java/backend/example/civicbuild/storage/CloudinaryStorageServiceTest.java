package backend.example.civicbuild.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.example.civicbuild.storage.exception.StorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloudinaryStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new CloudinaryStorageService(cloudinary);
    }

    @Test
    void uploadPrivateDocument_passesByteArrayNotInputStream() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        byte[] bytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
        when(uploader.upload(any(), any()))
                .thenReturn(Map.of(
                        "public_id", "verification-docs/user/doc",
                        "resource_type", "image",
                        "format", "png"));

        StoredFile stored = storageService.uploadPrivateDocument(bytes, "verification-docs/user/doc", DetectedFileType.PNG);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(uploader).upload(payload.capture(), any());
        assertThat(payload.getValue()).isInstanceOf(byte[].class);
        assertThat(stored.publicId()).isEqualTo("verification-docs/user/doc");
        assertThat(stored.format()).isEqualTo("png");
    }

    @Test
    void uploadPrivateDocument_rejectsEmptyPayload() {
        assertThatThrownBy(() -> storageService.uploadPrivateDocument(new byte[0], "id", DetectedFileType.PNG))
                .isInstanceOf(StorageException.class);
    }
}
