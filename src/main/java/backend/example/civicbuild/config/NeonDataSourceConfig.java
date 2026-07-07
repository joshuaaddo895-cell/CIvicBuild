package backend.example.civicbuild.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Builds the JDBC {@link DataSource} from the Neon-provided connection URL.
 *
 * <p>Neon (and most managed Postgres providers) hand out a libpq-style URL of the form
 * {@code postgresql://user:password@host/db?sslmode=require}. The PostgreSQL JDBC driver,
 * however, requires the {@code jdbc:postgresql://} scheme and credentials supplied separately.
 * We parse the provider URL here instead of committing a literal JDBC URL, so the only thing
 * that ever lives in configuration is the {@code NEON_DATABASE_URL} environment variable.
 */
@Configuration
public class NeonDataSourceConfig {

    private final String neonDatabaseUrl;

    public NeonDataSourceConfig(@Value("${NEON_DATABASE_URL}") String neonDatabaseUrl) {
        this.neonDatabaseUrl = neonDatabaseUrl;
    }

    @Bean
    public DataSource dataSource() {
        ParsedConnection connection = parse(neonDatabaseUrl);
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(connection.jdbcUrl())
                .username(connection.username())
                .password(connection.password())
                .build();
    }

    static ParsedConnection parse(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalStateException("NEON_DATABASE_URL is not set");
        }
        try {
            URI uri = new URI(rawUrl);
            String userInfo = uri.getUserInfo();
            if (!StringUtils.hasText(userInfo) || !userInfo.contains(":")) {
                throw new IllegalStateException(
                        "NEON_DATABASE_URL must include credentials (user:password@host)");
            }
            int separator = userInfo.indexOf(':');
            String username = userInfo.substring(0, separator);
            String password = userInfo.substring(separator + 1);

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
            jdbc.append(uri.getHost());
            if (uri.getPort() != -1) {
                jdbc.append(':').append(uri.getPort());
            }
            jdbc.append(uri.getPath());
            if (StringUtils.hasText(uri.getQuery())) {
                jdbc.append('?').append(uri.getQuery());
            }
            return new ParsedConnection(jdbc.toString(), username, password);
        } catch (URISyntaxException e) {
            // Do not include the raw URL (it contains the password) in the message.
            throw new IllegalStateException("NEON_DATABASE_URL is not a valid URL", e);
        }
    }

    record ParsedConnection(String jdbcUrl, String username, String password) {}
}
