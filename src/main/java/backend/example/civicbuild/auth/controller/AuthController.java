package backend.example.civicbuild.auth.controller;

import backend.example.civicbuild.auth.dto.AuthResponse;
import backend.example.civicbuild.auth.dto.ForgotPasswordRequest;
import backend.example.civicbuild.auth.dto.GoogleSignInRequest;
import backend.example.civicbuild.auth.dto.LoginRequest;
import backend.example.civicbuild.auth.dto.LogoutRequest;
import backend.example.civicbuild.auth.dto.RefreshRequest;
import backend.example.civicbuild.auth.dto.RegisterRequest;
import backend.example.civicbuild.auth.dto.ResetPasswordRequest;
import backend.example.civicbuild.auth.dto.UserResponse;
import backend.example.civicbuild.auth.service.AuthService;
import backend.example.civicbuild.auth.service.GoogleAuthService;
import backend.example.civicbuild.auth.service.PasswordResetService;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.common.web.ClientIpResolver;
import backend.example.civicbuild.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACTION_REGISTER = "register";
    private static final String ACTION_LOGIN = "login";
    private static final String ACTION_GOOGLE = "google";

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordResetService;
    private final RateLimiterService rateLimiter;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService,
            PasswordResetService passwordResetService, RateLimiterService rateLimiter) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        rateLimiter.checkAndConsume(ACTION_REGISTER, ClientIpResolver.resolve(httpRequest), request.email());
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful. Please sign in.", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        rateLimiter.checkAndConsume(ACTION_LOGIN, ClientIpResolver.resolve(httpRequest), request.email());
        AuthResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleSignIn(
            @Valid @RequestBody GoogleSignInRequest request, HttpServletRequest httpRequest) {
        // Email is inside the verified token; rate-limit by IP + action until verified.
        rateLimiter.checkAndConsume(ACTION_GOOGLE, ClientIpResolver.resolve(httpRequest), ACTION_GOOGLE);
        AuthResponse tokens = googleAuthService.signInWithGoogle(request.idToken());
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        // Always generic — never reveal whether the email exists.
        return ResponseEntity.ok(ApiResponse.message(
                "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.message("Password has been reset. Please sign in."));
    }
}
