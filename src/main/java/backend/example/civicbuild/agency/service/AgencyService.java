package backend.example.civicbuild.agency.service;

import backend.example.civicbuild.agency.dto.AgencyPostResponse;
import backend.example.civicbuild.agency.dto.AgencyResponse;
import backend.example.civicbuild.agency.dto.CreateAgencyRequest;
import backend.example.civicbuild.agency.dto.CreatePostRequest;
import backend.example.civicbuild.agency.dto.PersonnelResponse;
import backend.example.civicbuild.agency.dto.UpdateAgencyRequest;
import backend.example.civicbuild.agency.dto.UpdatePostRequest;
import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.entity.AgencyPost;
import backend.example.civicbuild.agency.exception.AgencyAccessDeniedException;
import backend.example.civicbuild.agency.exception.AgencyAlreadyExistsException;
import backend.example.civicbuild.agency.exception.AgencyNotFoundException;
import backend.example.civicbuild.agency.exception.AgencyPostNotFoundException;
import backend.example.civicbuild.agency.repository.AgencyPostRepository;
import backend.example.civicbuild.agency.repository.AgencyRepository;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.auth.security.UserRoleResolver;
import backend.example.civicbuild.common.dto.PageResponse;
import backend.example.civicbuild.common.web.PaginationSupport;
import backend.example.civicbuild.common.web.SearchSupport;
import backend.example.civicbuild.delivery.entity.DeliveryApprovalStatus;
import backend.example.civicbuild.delivery.entity.DeliveryProvider;
import backend.example.civicbuild.delivery.repository.DeliveryProviderRepository;
import backend.example.civicbuild.notification.entity.NotificationType;
import backend.example.civicbuild.notification.service.NotificationService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final AgencyPostRepository postRepository;
    private final UserRepository userRepository;
    private final DeliveryProviderRepository deliveryProviderRepository;
    private final NotificationService notificationService;
    private final UserRoleResolver userRoleResolver;
    private final Clock clock;

    public AgencyService(
            AgencyRepository agencyRepository,
            AgencyPostRepository postRepository,
            UserRepository userRepository,
            DeliveryProviderRepository deliveryProviderRepository,
            NotificationService notificationService,
            UserRoleResolver userRoleResolver,
            Clock clock) {
        this.agencyRepository = agencyRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.deliveryProviderRepository = deliveryProviderRepository;
        this.notificationService = notificationService;
        this.userRoleResolver = userRoleResolver;
        this.clock = clock;
    }

    @Transactional
    public AgencyResponse createAgency(AuthenticatedUser actor, CreateAgencyRequest request) {
        requireConstructionRole(actor);
        User owner = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        if (agencyRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new AgencyAlreadyExistsException();
        }
        Agency agency = agencyRepository.save(Agency.builder()
                .owner(owner)
                .name(request.name().trim())
                .category(request.category().trim())
                .tagline(request.tagline())
                .description(request.description())
                .address(request.address())
                .phone(request.phone())
                .hours(request.hours())
                .services(joinServices(request.services()))
                .build());
        return AgencyResponse.from(agency);
    }

    @Transactional(readOnly = true)
    public AgencyResponse getMyAgency(AuthenticatedUser actor) {
        Agency agency = requireOwnedAgency(actor);
        return AgencyResponse.from(agency);
    }

    @Transactional
    public AgencyResponse updateMyAgency(AuthenticatedUser actor, UpdateAgencyRequest request) {
        Agency agency = requireOwnedAgency(actor);
        if (request.name() != null) agency.setName(request.name().trim());
        if (request.category() != null) agency.setCategory(request.category().trim());
        if (request.logoUrl() != null) agency.setLogoUrl(request.logoUrl());
        if (request.tagline() != null) agency.setTagline(request.tagline());
        if (request.description() != null) agency.setDescription(request.description());
        if (request.address() != null) agency.setAddress(request.address());
        if (request.phone() != null) agency.setPhone(request.phone());
        if (request.hours() != null) agency.setHours(request.hours());
        if (request.services() != null) agency.setServices(joinServices(request.services()));
        return AgencyResponse.from(agencyRepository.save(agency));
    }

    @Transactional(readOnly = true)
    public PageResponse<AgencyResponse> listAgencies(String q, Integer page, Integer limit) {
        Pageable pageable = PaginationSupport.pageable(page, limit);
        Page<Agency> result = agencyRepository.search(SearchSupport.likePattern(q), pageable);
        List<AgencyResponse> items = result.getContent().stream().map(AgencyResponse::from).toList();
        return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AgencyResponse getAgency(UUID agencyId) {
        Agency agency = agencyRepository.findById(agencyId).orElseThrow(AgencyNotFoundException::new);
        return AgencyResponse.from(agency);
    }

    @Transactional
    public AgencyPostResponse createPost(AuthenticatedUser actor, CreatePostRequest request) {
        Agency agency = requireOwnedAgency(actor);
        AgencyPost post = postRepository.save(AgencyPost.builder()
                .agency(agency)
                .type(request.type())
                .title(request.title().trim())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .build());
        return AgencyPostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgencyPostResponse> listMyPosts(AuthenticatedUser actor, Integer page, Integer limit) {
        Agency agency = requireOwnedAgency(actor);
        return listAgencyPosts(agency.getId(), page, limit);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgencyPostResponse> listAgencyPosts(UUID agencyId, Integer page, Integer limit) {
        Pageable pageable = PaginationSupport.pageable(page, limit);
        Page<AgencyPost> result = postRepository.findByAgencyIdOrderByCreatedAtDesc(agencyId, pageable);
        List<AgencyPostResponse> items = result.getContent().stream().map(AgencyPostResponse::from).toList();
        return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public AgencyPostResponse updatePost(AuthenticatedUser actor, UUID postId, UpdatePostRequest request) {
        Agency agency = requireOwnedAgency(actor);
        AgencyPost post = postRepository
                .findByIdAndAgencyId(postId, agency.getId())
                .orElseThrow(AgencyPostNotFoundException::new);
        if (request.type() != null) post.setType(request.type());
        if (request.title() != null) post.setTitle(request.title().trim());
        if (request.description() != null) post.setDescription(request.description());
        if (request.imageUrl() != null) post.setImageUrl(request.imageUrl());
        return AgencyPostResponse.from(postRepository.save(post));
    }

    @Transactional
    public void deletePost(AuthenticatedUser actor, UUID postId) {
        Agency agency = requireOwnedAgency(actor);
        AgencyPost post = postRepository
                .findByIdAndAgencyId(postId, agency.getId())
                .orElseThrow(AgencyPostNotFoundException::new);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PersonnelResponse> listPersonnel(AuthenticatedUser actor) {
        Agency agency = requireOwnedAgency(actor);
        return listPersonnelForAgency(agency.getId(), false);
    }

    @Transactional(readOnly = true)
    public List<PersonnelResponse> listPublicPersonnel(UUID agencyId) {
        if (!agencyRepository.existsById(agencyId)) {
            throw new AgencyNotFoundException();
        }
        return listPersonnelForAgency(agencyId, true);
    }

    private List<PersonnelResponse> listPersonnelForAgency(UUID agencyId, boolean approvedOnly) {
        return deliveryProviderRepository.findByConstructionAgencyIdOrderBySubmittedAtDesc(agencyId).stream()
                .filter(dp -> !approvedOnly || dp.getApprovalStatus() == DeliveryApprovalStatus.approved)
                .map(this::toPersonnel)
                .toList();
    }

    @Transactional
    public PersonnelResponse approvePersonnel(AuthenticatedUser actor, UUID personnelId) {
        Agency agency = requireOwnedAgency(actor);
        DeliveryProvider provider = deliveryProviderRepository
                .findById(personnelId)
                .filter(dp -> dp.getConstructionAgency() != null
                        && dp.getConstructionAgency().getId().equals(agency.getId()))
                .orElseThrow(AgencyNotFoundException::new);
        provider.setApprovalStatus(DeliveryApprovalStatus.approved);
        provider.setHandledAt(clock.instant());
        deliveryProviderRepository.save(provider);
        notificationService.notify(
                provider.getUser(),
                NotificationType.personnel,
                "Association approved",
                "Your association with " + agency.getName() + " has been approved.",
                java.util.Map.of("agencyId", agency.getId().toString()));
        return toPersonnel(provider);
    }

    @Transactional
    public PersonnelResponse rejectPersonnel(AuthenticatedUser actor, UUID personnelId) {
        Agency agency = requireOwnedAgency(actor);
        DeliveryProvider provider = deliveryProviderRepository
                .findById(personnelId)
                .filter(dp -> dp.getConstructionAgency() != null
                        && dp.getConstructionAgency().getId().equals(agency.getId()))
                .orElseThrow(AgencyNotFoundException::new);
        provider.setApprovalStatus(DeliveryApprovalStatus.rejected);
        provider.setHandledAt(clock.instant());
        deliveryProviderRepository.save(provider);
        return toPersonnel(provider);
    }

    @Transactional
    public void removePersonnel(AuthenticatedUser actor, UUID personnelId) {
        Agency agency = requireOwnedAgency(actor);
        DeliveryProvider provider = deliveryProviderRepository
                .findById(personnelId)
                .filter(dp -> dp.getConstructionAgency() != null
                        && dp.getConstructionAgency().getId().equals(agency.getId()))
                .orElseThrow(AgencyNotFoundException::new);
        provider.setConstructionAgency(null);
        provider.setApprovalStatus(DeliveryApprovalStatus.pending);
        provider.setHandledAt(clock.instant());
        deliveryProviderRepository.save(provider);
    }

    public Agency requireOwnedAgency(AuthenticatedUser actor) {
        requireConstructionRole(actor);
        return agencyRepository.findByOwnerId(actor.id()).orElseThrow(AgencyNotFoundException::new);
    }

    private void requireConstructionRole(AuthenticatedUser actor) {
        if (!userRoleResolver.hasAnyRole(actor, Role.CONSTRUCTION_AGENCY, Role.ADMIN)) {
            throw new AgencyAccessDeniedException();
        }
    }

    private PersonnelResponse toPersonnel(DeliveryProvider provider) {
        UUID agencyId = provider.getConstructionAgency() != null
                ? provider.getConstructionAgency().getId()
                : null;
        return new PersonnelResponse(
                provider.getId(),
                provider.getUser().getId(),
                provider.getFullName(),
                provider.getProfileImageUrl(),
                agencyId,
                provider.getVehicleInfo(),
                provider.getApprovalStatus().name(),
                provider.getSubmittedAt(),
                provider.getHandledAt());
    }

    private static String joinServices(List<String> services) {
        if (services == null || services.isEmpty()) {
            return null;
        }
        return String.join(",", services);
    }
}
