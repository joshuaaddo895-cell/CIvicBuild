package backend.example.civicbuild.agency.controller;

import backend.example.civicbuild.agency.dto.PortfolioUploadResponse;
import backend.example.civicbuild.agency.service.AgencyPortfolioService;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/agency/portfolio")
public class AgencyPortfolioController {

    private final AgencyPortfolioService agencyPortfolioService;

    public AgencyPortfolioController(AgencyPortfolioService agencyPortfolioService) {
        this.agencyPortfolioService = agencyPortfolioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<PortfolioUploadResponse>> uploadPortfolioImage(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestPart("file") MultipartFile file) {
        PortfolioUploadResponse response = agencyPortfolioService.uploadPortfolioImage(user, file);
        return ResponseEntity.ok(ApiResponse.ok("Portfolio image uploaded successfully", response));
    }
}
