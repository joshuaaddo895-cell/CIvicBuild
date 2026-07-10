package backend.example.civicbuild.delivery.service;

import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.exception.AgencyNotFoundException;
import backend.example.civicbuild.agency.repository.AgencyRepository;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.auth.security.UserRoleResolver;
import backend.example.civicbuild.common.exception.ForbiddenException;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.delivery.dto.DeliveryJobResponse;
import backend.example.civicbuild.delivery.dto.DeliveryProviderResponse;
import backend.example.civicbuild.delivery.dto.DeliveryProviderSetupRequest;
import backend.example.civicbuild.delivery.entity.DeliveryApprovalStatus;
import backend.example.civicbuild.delivery.entity.DeliveryJob;
import backend.example.civicbuild.delivery.entity.DeliveryProvider;
import backend.example.civicbuild.delivery.repository.DeliveryJobRepository;
import backend.example.civicbuild.delivery.repository.DeliveryProviderRepository;
import backend.example.civicbuild.notification.entity.NotificationType;
import backend.example.civicbuild.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryProviderService {

    private final DeliveryProviderRepository providerRepository;
    private final DeliveryJobRepository jobRepository;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final NotificationService notificationService;
    private final UserRoleResolver userRoleResolver;

    public DeliveryProviderService(
            DeliveryProviderRepository providerRepository,
            DeliveryJobRepository jobRepository,
            UserRepository userRepository,
            AgencyRepository agencyRepository,
            NotificationService notificationService,
            UserRoleResolver userRoleResolver) {
        this.providerRepository = providerRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.notificationService = notificationService;
        this.userRoleResolver = userRoleResolver;
    }

    @Transactional
    public DeliveryProviderResponse setup(AuthenticatedUser actor, DeliveryProviderSetupRequest request) {
        requireDeliveryRole(actor);
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        Agency agency = null;
        if (request.constructionAgencyId() != null) {
            agency = agencyRepository
                    .findById(request.constructionAgencyId())
                    .orElseThrow(AgencyNotFoundException::new);
        }
        DeliveryProvider provider = providerRepository
                .findByUserId(user.getId())
                .orElseGet(() -> DeliveryProvider.builder().user(user).build());
        provider.setFullName(request.fullName().trim());
        provider.setVehicleInfo(request.vehicleInfo());
        provider.setProfileImageUrl(request.profileImageUrl());
        provider.setConstructionAgency(agency);
        provider.setApprovalStatus(DeliveryApprovalStatus.pending);
        provider = providerRepository.save(provider);
        if (agency != null) {
            notificationService.notify(
                    agency.getOwner(),
                    NotificationType.personnel,
                    "New personnel request",
                    request.fullName() + " requested to join your agency.",
                    java.util.Map.of("personnelId", provider.getId().toString()));
        }
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public DeliveryProviderResponse getMe(AuthenticatedUser actor) {
        DeliveryProvider provider = providerRepository
                .findByUserId(actor.id())
                .orElseThrow(() -> new NotFoundException("Delivery profile not found"));
        return toResponse(provider);
    }

    @Transactional
    public DeliveryProviderResponse updateMe(AuthenticatedUser actor, DeliveryProviderSetupRequest request) {
        DeliveryProvider provider = providerRepository
                .findByUserId(actor.id())
                .orElseThrow(() -> new NotFoundException("Delivery profile not found"));
        if (request.fullName() != null) provider.setFullName(request.fullName().trim());
        if (request.vehicleInfo() != null) provider.setVehicleInfo(request.vehicleInfo());
        if (request.profileImageUrl() != null) provider.setProfileImageUrl(request.profileImageUrl());
        if (request.constructionAgencyId() != null) {
            Agency agency = agencyRepository
                    .findById(request.constructionAgencyId())
                    .orElseThrow(AgencyNotFoundException::new);
            provider.setConstructionAgency(agency);
            provider.setApprovalStatus(DeliveryApprovalStatus.pending);
        }
        return toResponse(providerRepository.save(provider));
    }

    @Transactional
    public void removeAssociation(AuthenticatedUser actor) {
        DeliveryProvider provider = providerRepository
                .findByUserId(actor.id())
                .orElseThrow(() -> new NotFoundException("Delivery profile not found"));
        provider.setConstructionAgency(null);
        provider.setApprovalStatus(DeliveryApprovalStatus.pending);
        providerRepository.save(provider);
    }

    @Transactional(readOnly = true)
    public List<DeliveryJobResponse> listJobs(AuthenticatedUser actor) {
        DeliveryProvider provider = providerRepository
                .findByUserId(actor.id())
                .orElseThrow(() -> new NotFoundException("Delivery profile not found"));
        return jobRepository.findByDeliveryProviderIdOrderByAssignedAtDesc(provider.getId()).stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional
    public DeliveryJobResponse updateJobStatus(AuthenticatedUser actor, UUID jobId, String status) {
        DeliveryProvider provider = providerRepository
                .findByUserId(actor.id())
                .orElseThrow(() -> new NotFoundException("Delivery profile not found"));
        DeliveryJob job = jobRepository
                .findByIdAndDeliveryProviderId(jobId, provider.getId())
                .orElseThrow(() -> new NotFoundException("Job not found"));
        job.setStatus(DeliveryJob.JobStatus.valueOf(status));
        return toJobResponse(jobRepository.save(job));
    }

    private void requireDeliveryRole(AuthenticatedUser actor) {
        if (!userRoleResolver.hasAnyRole(actor, Role.DELIVERY_PROVIDER, Role.ADMIN)) {
            throw new ForbiddenException("Delivery provider role required");
        }
    }

    private DeliveryProviderResponse toResponse(DeliveryProvider provider) {
        UUID agencyId = provider.getConstructionAgency() != null
                ? provider.getConstructionAgency().getId()
                : null;
        return new DeliveryProviderResponse(
                provider.getId(),
                provider.getUser().getId(),
                provider.getFullName(),
                agencyId,
                provider.getVehicleInfo(),
                provider.getProfileImageUrl(),
                provider.getApprovalStatus().name(),
                provider.getSubmittedAt(),
                provider.getHandledAt());
    }

    private DeliveryJobResponse toJobResponse(DeliveryJob job) {
        String orderNumber = job.getOrder().getPaystackReference();
        return new DeliveryJobResponse(
                job.getId(),
                job.getOrder().getId(),
                orderNumber,
                job.getPickupAddress(),
                job.getDeliveryAddress(),
                job.getStatus().name(),
                job.getAssignedAt());
    }
}
