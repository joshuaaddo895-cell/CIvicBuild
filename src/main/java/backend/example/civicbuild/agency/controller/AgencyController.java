package backend.example.civicbuild.agency.controller;

import backend.example.civicbuild.agency.dto.AgencyPostResponse;
import backend.example.civicbuild.agency.dto.AgencyResponse;
import backend.example.civicbuild.agency.dto.CreateAgencyRequest;
import backend.example.civicbuild.agency.dto.CreatePostRequest;
import backend.example.civicbuild.agency.dto.PersonnelResponse;
import backend.example.civicbuild.agency.dto.PortfolioImageResponse;
import backend.example.civicbuild.agency.dto.UpdateAgencyRequest;
import backend.example.civicbuild.agency.dto.UpdatePostRequest;
import backend.example.civicbuild.agency.exception.AgencyNotFoundException;
import backend.example.civicbuild.agency.repository.AgencyRepository;
import backend.example.civicbuild.agency.service.AgencyPortfolioService;
import backend.example.civicbuild.agency.service.AgencyService;
import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agencies")
public class AgencyController {

    private final AgencyService agencyService;
    private final AgencyPortfolioService agencyPortfolioService;
    private final AgencyRepository agencyRepository;

    public AgencyController(
            AgencyService agencyService,
            AgencyPortfolioService agencyPortfolioService,
            AgencyRepository agencyRepository) {
        this.agencyService = agencyService;
        this.agencyPortfolioService = agencyPortfolioService;
        this.agencyRepository = agencyRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgencyResponse>> createAgency(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateAgencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Agency created", agencyService.createAgency(user, request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AgencyResponse>> getMyAgency(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.getMyAgency(user)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<AgencyResponse>> updateMyAgency(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateAgencyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Agency updated", agencyService.updateMyAgency(user, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AgencyResponse>>> listAgencies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.listAgencies(q, page, limit)));
    }

    @GetMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyResponse>> getAgency(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.getAgency(agencyId)));
    }

    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<PageResponse<AgencyPostResponse>>> listMyPosts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.listMyPosts(user, page, limit)));
    }

    @GetMapping("/{agencyId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<AgencyPostResponse>>> listAgencyPosts(
            @PathVariable UUID agencyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.listAgencyPosts(agencyId, page, limit)));
    }

    @PostMapping("/me/posts")
    public ResponseEntity<ApiResponse<AgencyPostResponse>> createPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Post created", agencyService.createPost(user, request)));
    }

    @PatchMapping("/me/posts/{postId}")
    public ResponseEntity<ApiResponse<AgencyPostResponse>> updatePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Post updated", agencyService.updatePost(user, postId, request)));
    }

    @DeleteMapping("/me/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID postId) {
        agencyService.deletePost(user, postId);
        return ResponseEntity.ok(ApiResponse.message("Post deleted"));
    }

    @GetMapping("/me/personnel")
    public ResponseEntity<ApiResponse<List<PersonnelResponse>>> listPersonnel(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.listPersonnel(user)));
    }

    @PostMapping("/me/personnel/{personnelId}/approve")
    public ResponseEntity<ApiResponse<PersonnelResponse>> approvePersonnel(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID personnelId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Personnel approved", agencyService.approvePersonnel(user, personnelId)));
    }

    @PostMapping("/me/personnel/{personnelId}/reject")
    public ResponseEntity<ApiResponse<PersonnelResponse>> rejectPersonnel(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID personnelId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Personnel rejected", agencyService.rejectPersonnel(user, personnelId)));
    }

    @DeleteMapping("/me/personnel/{personnelId}")
    public ResponseEntity<ApiResponse<Void>> removePersonnel(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID personnelId) {
        agencyService.removePersonnel(user, personnelId);
        return ResponseEntity.ok(ApiResponse.message("Personnel removed"));
    }

    @GetMapping("/me/portfolio")
    public ResponseEntity<ApiResponse<List<PortfolioImageResponse>>> listMyPortfolio(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(agencyPortfolioService.listMyPortfolio(user)));
    }

    @GetMapping("/{agencyId}/portfolio")
    public ResponseEntity<ApiResponse<List<PortfolioImageResponse>>> listAgencyPortfolio(
            @PathVariable UUID agencyId) {
        Agency agency = agencyRepository.findById(agencyId).orElseThrow(AgencyNotFoundException::new);
        return ResponseEntity.ok(ApiResponse.ok(
                agencyPortfolioService.listAgencyPortfolio(agency.getOwner().getId())));
    }

    @DeleteMapping("/me/portfolio/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolioImage(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID imageId) {
        agencyPortfolioService.deletePortfolioImage(user, imageId);
        return ResponseEntity.ok(ApiResponse.message("Portfolio image deleted"));
    }
}
