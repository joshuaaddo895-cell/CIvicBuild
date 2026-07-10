package backend.example.civicbuild.onboarding.dto;

import backend.example.civicbuild.onboarding.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingRequest(@NotNull AccountType accountType) {}
