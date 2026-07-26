package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ValidationRuleSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsApprovedColumnsConstraintsAndIndexes() {
        assertThat(
                columnNames())
                .containsExactly(
                        "rule_code",
                        "rule_version",
                        "name",
                        "description",
                        "scope",
                        "severity",
                        "failure_effect",
                        "is_current",
                        "created_at");

        assertThat(
                constraintNames())
                .contains(
                        "pk_validation_rule",
                        "ck_validation_rule_rule_code",
                        "ck_validation_rule_rule_version",
                        "ck_validation_rule_name",
                        "ck_validation_rule_description",
                        "ck_validation_rule_scope",
                        "ck_validation_rule_severity",
                        "ck_validation_rule_failure_effect");

        assertThat(
                indexNames())
                .contains(
                        "uq_validation_rule_current",
                        "idx_validation_rule_scope_current");
    }

    @Test
    void loadsTheFixedVersionedMvpCatalog() {
        List<String> rules =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            rule_code
                            || '|'
                            || rule_version
                            || '|'
                            || scope
                            || '|'
                            || severity
                            || '|'
                            || failure_effect
                            || '|'
                            || is_current
                        FROM ocv.validation_rule
                        ORDER BY rule_code
                        """,
                        String.class);

        assertThat(rules)
                .containsExactly(
                        "VR-001|1|EVENT|CRITICAL|BLOCKS_CLOSE|true",
                        "VR-002|1|EVENT|CRITICAL|BLOCKS_EVENT_AND_CLOSE|true",
                        "VR-003|1|EVENT|HIGH|BLOCKS_EVENT_AND_CLOSE|true",
                        "VR-006|1|EVENT|CRITICAL|BLOCKS_EVENT_AND_CLOSE|true",
                        "VR-008|1|CLOSE|CRITICAL|REJECTS_SUBMISSION_AND_BLOCKS_CLOSE|true");
    }

    @Test
    void permitsHistoricalVersionsButOnlyOneCurrentVersionPerCode() {
        jdbcTemplate.update(
                """
                INSERT INTO ocv.validation_rule (
                    rule_code,
                    rule_version,
                    name,
                    description,
                    scope,
                    severity,
                    failure_effect,
                    is_current,
                    created_at
                )
                VALUES (
                    'VR-001',
                    2,
                    'Historical test version',
                    'Historical non-current definition used by the schema test.',
                    'EVENT',
                    'CRITICAL',
                    'BLOCKS_CLOSE',
                    FALSE,
                    CURRENT_TIMESTAMP
                )
                """);

        Integer historicalVersions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_rule
                        WHERE rule_code = 'VR-001'
                          AND is_current = FALSE
                        """,
                        Integer.class);

        assertThat(historicalVersions)
                .isEqualTo(
                        1);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO ocv.validation_rule (
                            rule_code,
                            rule_version,
                            name,
                            description,
                            scope,
                            severity,
                            failure_effect,
                            is_current,
                            created_at
                        )
                        VALUES (
                            'VR-001',
                            3,
                            'Second current version',
                            'This insert must violate the current-version index.',
                            'EVENT',
                            'CRITICAL',
                            'BLOCKS_CLOSE',
                            TRUE,
                            CURRENT_TIMESTAMP
                        )
                        """))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        jdbcTemplate.update(
                """
                DELETE FROM ocv.validation_rule
                WHERE rule_code = 'VR-001'
                  AND rule_version = 2
                """);
    }

    private List<String> columnNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'ocv'
                  AND table_name = 'validation_rule'
                ORDER BY ordinal_position
                """,
                String.class);
    }

    private List<String> constraintNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'ocv'
                  AND table_name = 'validation_rule'
                ORDER BY constraint_name
                """,
                String.class);
    }

    private List<String> indexNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'ocv'
                  AND tablename = 'validation_rule'
                ORDER BY indexname
                """,
                String.class);
    }

}