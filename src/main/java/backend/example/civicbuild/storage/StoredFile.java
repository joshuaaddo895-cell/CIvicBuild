package backend.example.civicbuild.storage;

/**
 * Result of a successful Cloudinary upload. Callers persist {@link #publicId()} and
 * {@link #resourceType()} — not client-supplied filenames.
 */
public record StoredFile(String publicId, String resourceType, String format, String deliveryUrl) {}
