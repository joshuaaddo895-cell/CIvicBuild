package backend.example.civicbuild.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NeonDataSourceConfigTest {

    @Test
    void parse_convertsPostgresqlUrlToJdbcForm() {
        NeonDataSourceConfig.ParsedConnection parsed = NeonDataSourceConfig.parse(
                "postgresql://user:secret@db.example.com:5432/mydb?sslmode=require");

        assertThat(parsed.jdbcUrl())
                .isEqualTo("jdbc:postgresql://db.example.com:5432/mydb?sslmode=require");
        assertThat(parsed.username()).isEqualTo("user");
        assertThat(parsed.password()).isEqualTo("secret");
    }

    @Test
    void parse_handlesUrlWithoutExplicitPort() {
        NeonDataSourceConfig.ParsedConnection parsed = NeonDataSourceConfig.parse(
                "postgresql://user:secret@db.example.com/mydb");

        assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://db.example.com/mydb");
    }

    @Test
    void parse_rejectsMissingCredentials() {
        assertThatThrownBy(() -> NeonDataSourceConfig.parse("postgresql://db.example.com/mydb"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void parse_rejectsBlankUrl() {
        assertThatThrownBy(() -> NeonDataSourceConfig.parse("  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NEON_DATABASE_URL");
    }
}
