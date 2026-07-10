package backend.example.civicbuild.onboarding.service;

import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.repository.AgencyRepository;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.entity.VerificationStatus;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.delivery.entity.DeliveryApprovalStatus;
import backend.example.civicbuild.delivery.entity.DeliveryProvider;
import backend.example.civicbuild.delivery.repository.DeliveryProviderRepository;
import backend.example.civicbuild.onboarding.dto.DeliveryProviderProfileResponse;
import backend.example.civicbuild.onboarding.dto.OnboardingResponse;
import backend.example.civicbuild.onboarding.dto.UpdateOnboardingRequest;
import backend.example.civicbuild.onboarding.entity.AccountType;
import backend.example.civicbuild.onboarding.entity.UserOnboarding;
import backend.example.civicbuild.onboarding.repository.UserOnboardingRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

    private final UserOnboardingRepository onboardingRepository;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final DeliveryProviderRepository deliveryProviderRepository;

    public OnboardingService(
            UserOnboardingRepository onboardingRepository,
            UserRepository userRepository,
            AgencyRepository agencyRepository,
            DeliveryProviderRepository deliveryProviderRepository) {
        this.onboardingRepository = onboardingRepository;
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.deliveryProviderRepository = deliveryProviderRepository;
    }

    @Transactional(readOnly = true)
    public OnboardingResponse getOnboarding(AuthenticatedUser actor) {
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        UserOnboarding onboarding = onboardingRepository
                .findByUserId(user.getId())
                .orElseGet(() -> defaultOnboarding(user));
        return toResponse(user, onboarding);
    }

    @Transactional
    public OnboardingResponse updateOnboarding(AuthenticatedUser actor, UpdateOnboardingRequest request) {
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        UserOnboarding onboarding = onboardingRepository
                .findByUserId(user.getId())
                .orElseGet(() -> createOnboarding(user));

        onboarding.setAccountType(request.accountType());
        user.setRole(mapAccountTypeToRole(request.accountType()));
        onboardingRepository.save(onboarding);
        userRepository.save(user);
        return toResponse(user, onboarding);
    }

    @Transactional
    public OnboardingResponse completeOnboarding(AuthenticatedUser actor) {
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        UserOnboarding onboarding = onboardingRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Set account type before completing onboarding"));
        onboarding.setOnboardingComplete(true);
        onboardingRepository.save(onboarding);
        return toResponse(user, onboarding);
    }

    private UserOnboarding createOnboarding(User user) {
        return onboardingRepository.save(UserOnboarding.builder().user(user).build());
    }

    private UserOnboarding defaultOnboarding(User user) {
        return UserOnboarding.builder().user(user).build();
    }

    private Role mapAccountTypeToRole(AccountType accountType) {
        return switch (accountType) {
            case customer -> Role.CUSTOMER;
            case construction -> Role.CONSTRUCTION_AGENCY;
            case delivery -> Role.DELIVERY_PROVIDER;
        };
    }

    private OnboardingResponse toResponse(User user, UserOnboarding onboarding) {
        UUID managedAgencyId = agencyRepository.findByOwnerId(user.getId()).map(Agency::getId).orElse(null);
        DeliveryProviderProfileResponse profile = deliveryProviderRepository
                .findByUserId(user.getId())
                .map(this::toDeliveryProfile)
                .orElse(null);
        String deliveryStatus = deliveryProviderRepository
                .findByUserId(user.getId())
                .map(dp -> dp.getApprovalStatus().name())
                .orElse("none");

        return new OnboardingResponse(
                onboarding.getAccountType(),
                onboarding.isOnboardingComplete(),
                user.getVerificationStatus().name().toLowerCase(),
                managedAgencyId,
                profile,
                deliveryStatus);
    }

    private DeliveryProviderProfileResponse toDeliveryProfile(DeliveryProvider provider) {
        UUID agencyId =
                provider.getConstructionAgency() != null ? provider.getConstructionAgency().getId() : null;
        return new DeliveryProviderProfileResponse(
                provider.getFullName(),
                agencyId,
                provider.getVehicleInfo(),
                provider.getProfileImageUrl());
    }
}
