package backend.example.civicbuild.support;

import backend.example.civicbuild.CivicbuildApplication;
import backend.example.civicbuild.email.service.EmailService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers-backed Spring context for integration tests.
 * Provides isolated Postgres + Redis instances and mocks outbound email.
 */
@SpringBootTest(
        classes = CivicbuildApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    private static final DockerImageName POSTGRES = DockerImageName.parse("postgres:16-alpine");
    private static final DockerImageName REDIS = DockerImageName.parse("redis:7-alpine");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(REDIS).withExposedPorts(6379);

    @LocalServerPort
    protected int port;

    protected TestRestTemplate rest = new TestRestTemplate();

    /** Email is best-effort in production; never call Resend from tests. */
    @MockitoBean
    protected EmailService emailService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("NEON_DATABASE_URL", IntegrationTestBase::postgresUrl);
        registry.add("REDIS_URL", IntegrationTestBase::redisUrl);
    }

    protected String authUrl(String path) {
        return "http://localhost:" + port + "/api/auth" + path;
    }

    protected String orderUrl(String path) {
        return "http://localhost:" + port + "/api/orders" + path;
    }

    protected String accountUrl(String path) {
        return "http://localhost:" + port + "/api/account" + path;
    }

    protected String usersUrl(String path) {
        return "http://localhost:" + port + "/api/users" + path;
    }

    protected String paymentUrl(String path) {
        return "http://localhost:" + port + "/api/payments" + path;
    }

    protected String verificationUrl(String path) {
        return "http://localhost:" + port + "/api/verification" + path;
    }

    protected String agencyPortfolioUrl(String path) {
        return "http://localhost:" + port + "/api/agency/portfolio" + path;
    }

    protected HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static String postgresUrl() {
        return "postgresql://%s:%s@%s:%d/%s".formatted(
                postgres.getUsername(),
                postgres.getPassword(),
                postgres.getHost(),
                postgres.getMappedPort(5432),
                postgres.getDatabaseName());
    }

    private static String redisUrl() {
        return "redis://%s:%d".formatted(redis.getHost(), redis.getMappedPort(6379));
    }
}
