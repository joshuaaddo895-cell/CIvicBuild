package backend.example.civicbuild.onboarding.dto;

import backend.example.civicbuild.onboarding.entity.AccountType;
import java.util.UUID;

public record OnboardingResponse(
        AccountType accountType,
        boolean onboardingComplete,
        String verificationStatus,
        UUID managedAgencyId,
        DeliveryProviderProfileResponse deliveryProviderProfile,
        String deliveryProviderStatus) {}
