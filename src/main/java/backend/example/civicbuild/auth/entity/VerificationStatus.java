package backend.example.civicbuild.auth.entity;

/**
 * Account verification state. Persisted as a string for display (e.g. a future "Verified" badge).
 * Does not gate access to any endpoint — uploads and dashboard access are not blocked by status.
 */
public enum VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED
}
