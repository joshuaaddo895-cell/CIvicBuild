package backend.example.civicbuild.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a local {@code .env} file into {@link System#setProperty(String, String)} before Spring
 * Boot starts, so {@code ${NEON_DATABASE_URL}}, {@code ${JWT_SECRET}}, etc. resolve reliably during
 * {@code mvn spring-boot:run}. Existing OS environment variables always win (production-safe).
 */
public final class DotenvBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DotenvBootstrap.class);

    private DotenvBootstrap() {
    }

    public static void load() {
        String directory = System.getProperty("user.dir");
        Dotenv dotenv = Dotenv.configure()
                .directory(directory)
                .filename(".env")
                .ignoreIfMissing()
                .load();
        int loaded = 0;
        for (var entry : dotenv.entries()) {
            String key = entry.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
                loaded++;
            }
        }
        if (loaded > 0) {
            log.info("Loaded {} variable(s) from {}/.env (OS env vars take precedence)", loaded, directory);
        }
    }
}
