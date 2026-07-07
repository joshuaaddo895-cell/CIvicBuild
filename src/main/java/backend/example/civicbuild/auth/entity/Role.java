package backend.example.civicbuild.auth.entity;

/**
 * Account role. Persisted as a string. RBAC enforcement is intentionally out of scope for the
 * auth foundation session — roles are stored only. New users default to {@link #CUSTOMER};
 * role selection happens in a separate post-signup onboarding step.
 */
public enum Role {
    CUSTOMER,
    CONSTRUCTION_AGENCY,
    DELIVERY_PROVIDER
}
