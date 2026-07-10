package backend.example.civicbuild.agency.dto;

import backend.example.civicbuild.agency.entity.AgencyPost;
import backend.example.civicbuild.agency.entity.AgencyPostType;
import java.time.Instant;
import java.util.UUID;

public record AgencyPostResponse(
        UUID id,
        UUID agencyId,
        AgencyPostType type,
        String title,
        String description,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static AgencyPostResponse from(AgencyPost post) {
        return new AgencyPostResponse(
                post.getId(),
                post.getAgency().getId(),
                post.getType(),
                post.getTitle(),
                post.getDescription(),
                post.getImageUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
