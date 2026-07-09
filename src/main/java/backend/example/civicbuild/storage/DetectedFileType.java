package backend.example.civicbuild.storage;

/** Server-detected file types allowed for upload. */
public enum DetectedFileType {
    PDF("raw", "application/pdf"),
    JPEG("image", "image/jpeg"),
    PNG("image", "image/png");

    private final String cloudinaryResourceType;
    private final String mimeType;

    DetectedFileType(String cloudinaryResourceType, String mimeType) {
        this.cloudinaryResourceType = cloudinaryResourceType;
        this.mimeType = mimeType;
    }

    public String cloudinaryResourceType() {
        return cloudinaryResourceType;
    }

    public String mimeType() {
        return mimeType;
    }
}
