package backend.example.civicbuild;

import backend.example.civicbuild.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class CivicbuildApplicationTests extends IntegrationTestBase {

    @Test
    void contextLoads() {
        // Spring context starts with Testcontainers-backed Postgres + Redis.
    }
}
