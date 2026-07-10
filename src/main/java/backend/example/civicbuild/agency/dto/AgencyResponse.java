package backend.example.civicbuild.agency.dto;

import backend.example.civicbuild.agency.entity.Agency;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record AgencyResponse(
        UUID id,
        String name,
        String logoUrl,
        boolean verified,
        String tagline,
        String description,
        String address,
        String phone,
        String hours,
        List<String> services,
        String category) {

    public static AgencyResponse from(Agency agency) {
        List<String> services = agency.getServices() == null
                ? List.of()
                : Arrays.stream(agency.getServices().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return new AgencyResponse(
                agency.getId(),
                agency.getName(),
                agency.getLogoUrl(),
                agency.isVerified(),
                agency.getTagline(),
                agency.getDescription(),
                agency.getAddress(),
                agency.getPhone(),
                agency.getHours(),
                services,
                agency.getCategory());
    }
}
