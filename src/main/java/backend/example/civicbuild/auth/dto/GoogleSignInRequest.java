package backend.example.civicbuild.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleSignInRequest(
        @NotBlank(message = "Google ID token is required")
        String idToken) {
}
