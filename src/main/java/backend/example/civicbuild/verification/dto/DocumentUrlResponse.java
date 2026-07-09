package backend.example.civicbuild.verification.dto;

import java.time.Instant;

public record DocumentUrlResponse(String signedUrl, Instant expiresAt) {}
