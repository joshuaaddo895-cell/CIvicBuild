package backend.example.civicbuild.auth.dto;

import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.entity.VerificationStatus;
import java.time.Instant;
import java.util.UUID;

/** Safe, outward-facing view of a user. Never exposes the password hash. */
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        VerificationStatus verificationStatus,
        boolean active,
        String profilePictureUrl,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getVerificationStatus(),
                user.isActive(),
                user.getProfilePictureUrl(),
                user.getCreatedAt());
    }
}
