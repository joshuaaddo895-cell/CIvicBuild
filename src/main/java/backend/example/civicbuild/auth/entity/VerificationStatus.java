package backend.example.civicbuild.auth.entity;

/**
 * Account verification state. Persisted as a string. Verification workflow logic is out of scope
 * for this session; the field is stored only and defaults to {@link #UNVERIFIED}.
 */
public enum VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED
}
