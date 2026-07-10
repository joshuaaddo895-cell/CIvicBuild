package backend.example.civicbuild.agency.dto;

import backend.example.civicbuild.agency.entity.AgencyPostType;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        AgencyPostType type,
        @Size(max = 255) String title,
        String description,
        @Size(max = 512) String imageUrl) {}
