package backend.example.civicbuild.catalog.dto;

import backend.example.civicbuild.catalog.entity.Category;

public record CategoryResponse(String id, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
