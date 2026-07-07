package backend.example.civicbuild.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Deliberately contains ONLY the fields a client may set.
 *
 * <p>{@code confirmPassword} is a frontend-only concern and is intentionally absent — it can never
 * be bound here. {@code role} is intentionally absent too: role is chosen in a separate onboarding
 * step, so every new account defaults to CUSTOMER (mass-assignment protection by omission).
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number")
        String password) {
}
