package backend.example.civicbuild.auth.dto;

/**
 * Tokens returned on successful login/refresh.
 *
 * @param accessToken  short-lived signed JWT for authorizing API calls
 * @param refreshToken long-lived opaque token used to obtain new access tokens
 * @param tokenType    always "Bearer"
 * @param expiresIn    access-token lifetime in seconds
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
