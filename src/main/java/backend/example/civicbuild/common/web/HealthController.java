package backend.example.civicbuild.common.web;

import backend.example.civicbuild.common.dto.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight, public liveness endpoint. Deep dependency health (DB, Redis) is available via
 * Spring Boot Actuator at {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}
