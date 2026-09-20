package com.pulsepass.repository;

import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QT-001 y QT-002: el esquema lo construye Flyway, no Hibernate.
 */
class FlywayMigrationIT extends AbstractPersistenceIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @Test
    @DisplayName("QT-001: Flyway aplica V1, V2 y V3 desde una base vacia")
    void shouldApplyAllMigrations() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);

        assertThat(versions).contains("1", "2", "3");
    }

    @Test
    @DisplayName("QT-001: ninguna migracion quedo marcada como fallida")
    void shouldNotHaveFailedMigrations() {
        Integer failed = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false",
                Integer.class);

        assertThat(failed).isZero();
    }

    @Test
    @DisplayName("QT-002: Hibernate opera en modo validate, sin crear ni actualizar el esquema")
    void shouldRunHibernateInValidateMode() {
        // Si el mapeo JPA y las tablas creadas por Flyway no coincidieran,
        // el contexto de Spring ni siquiera habria arrancado.
        assertThat(ddlAuto).isEqualTo("validate");
    }

    @Test
    @DisplayName("V1 creo las siete tablas del modelo")
    void shouldCreateEveryTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);

        assertThat(tables).contains(
                "venues", "events", "artists", "event_artists",
                "users", "user_profiles", "tickets");
    }

    @Test
    @DisplayName("V2 inserto el catalogo inicial de cinco artistas")
    void shouldSeedArtistCatalog() {
        List<String> stageNames = jdbcTemplate.queryForList(
                "select stage_name from artists order by stage_name",
                String.class);

        assertThat(stageNames).containsExactly(
                "Caribbean Sound", "Digital Pulse", "Neon Waves", "Ocean Drive", "Solar Beat");
    }

    @Test
    @DisplayName("V3 agrego streaming_url a events sin tocar V1")
    void shouldAddStreamingUrlColumn() {
        Integer maxLength = jdbcTemplate.queryForObject("""
                select character_maximum_length
                from information_schema.columns
                where table_name = 'events' and column_name = 'streaming_url'
                """, Integer.class);

        assertThat(maxLength).isEqualTo(500);
    }
}
