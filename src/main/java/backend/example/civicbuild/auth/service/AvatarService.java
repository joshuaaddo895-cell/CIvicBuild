package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.dto.UserResponse;
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
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarService {

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileUploadValidator fileUploadValidator;

    public AvatarService(
            UserRepository userRepository,
            StorageService storageService,
            FileUploadValidator fileUploadValidator) {
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileUploadValidator = fileUploadValidator;
    }

    @Transactional
    public Map<String, String> uploadAvatar(AuthenticatedUser actor, MultipartFile file) {
        fileUploadValidator.validatePortfolioUpload(file);
        DetectedFileType fileType = fileUploadValidator.detectFileType(file);
        User user = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        String publicId = "avatars/" + user.getId() + "/" + UUID.randomUUID();
        try {
            StoredFile stored = storageService.uploadPublicImage(file.getBytes(), publicId, fileType);
            user.setProfilePictureUrl(stored.deliveryUrl());
            userRepository.save(user);
            return Map.of("profilePictureUrl", stored.deliveryUrl());
        } catch (IOException e) {
            throw new InvalidFileUploadException("Unable to read uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(AuthenticatedUser actor) {
        return userRepository.findById(actor.id()).map(UserResponse::from).orElseThrow(UserNotFoundException::new);
    }
}
