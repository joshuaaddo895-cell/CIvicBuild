package backend.example.civicbuild.common.web;

import backend.example.civicbuild.common.dto.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(Map.of(
                "name", "CivicBuild API",
                "status", "UP",
                "message", "Welcome to the CivicBuild API. The service is running successfully."
        ));
    }
}
