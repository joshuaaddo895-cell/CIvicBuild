package backend.example.civicbuild.agency.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateAgencyRequest(
        @Size(max = 200) String name,
        @Size(max = 50) String category,
        @Size(max = 512) String logoUrl,
        @Size(max = 300) String tagline,
        String description,
        @Size(max = 500) String address,
        @Size(max = 30) String phone,
        @Size(max = 200) String hours,
        List<String> services) {}
