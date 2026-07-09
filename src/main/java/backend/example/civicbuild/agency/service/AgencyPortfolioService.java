package backend.example.civicbuild.agency.service;

import backend.example.civicbuild.agency.dto.PortfolioUploadResponse;
import backend.example.civicbuild.agency.entity.AgencyPortfolioImage;
import backend.example.civicbuild.agency.exception.AgencyPortfolioAccessDeniedException;
import backend.example.civicbuild.agency.repository.AgencyPortfolioImageRepository;
import backend.example.civicbuild.auth.entity.Role;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.storage.DetectedFileType;
import backend.example.civicbuild.storage.FileUploadValidator;
import backend.example.civicbuild.storage.StorageService;
import backend.example.civicbuild.storage.StoredFile;
import backend.example.civicbuild.storage.exception.InvalidFileUploadException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AgencyPortfolioService {

    private static final Logger log = LoggerFactory.getLogger(AgencyPortfolioService.class);

    private final AgencyPortfolioImageRepository portfolioImageRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileUploadValidator fileUploadValidator;

    public AgencyPortfolioService(
            AgencyPortfolioImageRepository portfolioImageRepository,
            UserRepository userRepository,
            StorageService storageService,
            FileUploadValidator fileUploadValidator) {
        this.portfolioImageRepository = portfolioImageRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileUploadValidator = fileUploadValidator;
    }

    @Transactional
    public PortfolioUploadResponse uploadPortfolioImage(AuthenticatedUser actor, MultipartFile file) {
        fileUploadValidator.validatePortfolioUpload(file);
        DetectedFileType fileType = fileUploadValidator.detectFileType(file);

        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        if (user.getRole() != Role.CONSTRUCTION_AGENCY) {
            throw new AgencyPortfolioAccessDeniedException();
        }

        String publicId = fileUploadValidator.portfolioPublicId(user.getId());
        StoredFile stored;
        try {
            stored = storageService.uploadPublicImage(file.getInputStream(), publicId, fileType);
        } catch (IOException e) {
            throw new InvalidFileUploadException("Unable to read uploaded file");
        }

        AgencyPortfolioImage image = portfolioImageRepository.save(AgencyPortfolioImage.builder()
                .user(user)
                .cloudinaryPublicId(stored.publicId())
                .resourceType(stored.resourceType())
                .build());

        log.info("Stored agency portfolio image id={} for userId={}", image.getId(), user.getId());

        return new PortfolioUploadResponse(
                image.getId(), stored.publicId(), stored.resourceType(), stored.deliveryUrl());
    }
}
