package backend.example.civicbuild.auth.security;

/**
 * Claims extracted from a cryptographically verified Google ID token.
 * Never construct this from unverified client input.
 */
public record VerifiedGoogleProfile(
        String email,
        String fullName,
        String pictureUrl) {
}
