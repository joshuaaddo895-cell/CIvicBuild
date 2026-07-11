package backend.example.civicbuild.agency.dto;

import backend.example.civicbuild.agency.entity.AgencyPostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull AgencyPostType type,
        @NotBlank @Size(max = 255) String title,
        String description,
        @Size(max = 512) String imageUrl) {}
