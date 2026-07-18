package com.marceloituccayasi.ocv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OperationalCloseValidatorApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private Flyway flyway;

    @Test
    void contextLoadsWithTestProfileUtcAndFlywayMigration() {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
        assertThat(flyway.info().applied())
                .anyMatch(migration ->
                        "V000__initialize_database.sql".equals(migration.getScript()));
    }

}
