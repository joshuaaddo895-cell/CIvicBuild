package backend.example.civicbuild.auth.service;

import backend.example.civicbuild.auth.dto.UpdateProfileRequest;
import backend.example.civicbuild.auth.dto.UserResponse;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(AuthenticatedUser principal) {
        return userRepository.findById(principal.id())
                .map(UserResponse::from)
                .orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public UserResponse updateProfile(AuthenticatedUser principal, UpdateProfileRequest request) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(UserNotFoundException::new);

        user.setFullName(request.fullName().trim());
        if (request.profilePictureUrl() != null) {
            String trimmed = request.profilePictureUrl().trim();
            user.setProfilePictureUrl(StringUtils.hasText(trimmed) ? trimmed : null);
        }

        return UserResponse.from(userRepository.save(user));
    }
}
