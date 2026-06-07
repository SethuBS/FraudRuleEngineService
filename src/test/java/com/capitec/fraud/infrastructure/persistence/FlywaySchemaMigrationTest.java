package com.capitec.fraud.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywaySchemaMigrationTest
{

    private static final String POSTGRES_IMAGE = System.getProperty(
            "test.postgres.image",
            System.getenv().getOrDefault("TEST_POSTGRES_IMAGE", "postgres:16-alpine"));

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
            DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withStartupTimeout(Duration.ofMinutes(2));

    @Test
    void createsCleanSchemaWithConstraintsIndexesAndAuditableRows()
            throws SQLException
    {
        migrate();

        try (var connection = openConnection())
        {
            assertTablesExist(connection);
            assertColumnsExist(connection);
            assertConstraintsExist(connection);
            assertIndexesExist(connection);

            insertAuditRows(connection);

            assertDuplicateRaceProtection(connection);
            assertThat(countRows(connection, "SELECT COUNT(*) FROM rule_evaluations WHERE matched = TRUE")).isEqualTo(1);
            assertThat(countRows(connection, "SELECT COUNT(*) FROM rule_evaluations WHERE matched = FALSE")).isEqualTo(1);
            assertThat(countRows(connection, alertRetrievalQuery())).isEqualTo(1);
        }
    }

    private static void migrate()
    {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static Connection openConnection()
            throws SQLException
    {
        return DriverManager.getConnection(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
    }

    private static void assertTablesExist(Connection connection)
            throws SQLException
    {
        assertThat(tableNames(connection))
                .contains(
                        "processed_events",
                        "transactions",
                        "fraud_alerts",
                        "fraud_rules",
                        "rule_evaluations");
    }

    private static void assertColumnsExist(Connection connection)
            throws SQLException
    {
        assertThat(columnNames(connection, "transactions"))
                .contains(
                        "event_id",
                        "transaction_id",
                        "customer_id",
                        "account_id",
                        "amount",
                        "currency",
                        "transaction_timestamp",
                        "merchant_category",
                        "sanitized_raw_payload",
                        "raw_payload_expires_at",
                        "created_at",
                        "updated_at");
        assertThat(columnNames(connection, "processed_events"))
                .contains(
                        "event_id",
                        "evaluated_at",
                        "sanitized_raw_payload",
                        "raw_payload_expires_at",
                        "created_at",
                        "updated_at");
        assertThat(columnNames(connection, "fraud_rules"))
                .contains("code", "name", "description", "enabled", "severity", "score", "created_at", "updated_at");
        assertThat(columnNames(connection, "fraud_alerts"))
                .contains("transaction_id", "customer_id", "account_id", "risk_level", "created_at", "updated_at");
        assertThat(columnNames(connection, "rule_evaluations"))
                .contains("transaction_id", "rule_code", "matched", "score_contribution", "explanation", "created_at", "updated_at");
    }

    private static void assertConstraintsExist(Connection connection)
            throws SQLException
    {
        assertThat(constraintNames(connection))
                .contains(
                        "uk_processed_events_event_id",
                        "uk_transactions_transaction_id",
                        "fk_fraud_alerts_transactions",
                        "fk_rule_evaluations_transactions",
                        "ck_transactions_amount_positive",
                        "ck_rule_evaluations_score_non_negative");
    }

    private static void assertIndexesExist(Connection connection)
            throws SQLException
    {
        assertThat(indexNames(connection))
                .contains(
                        "uk_processed_events_event_id",
                        "uk_transactions_transaction_id",
                        "idx_transactions_customer_id",
                        "idx_transactions_account_id",
                        "idx_transactions_customer_transaction_timestamp",
                        "idx_transactions_account_transaction_timestamp",
                        "idx_transactions_created_at",
                        "idx_fraud_alerts_transaction_id",
                        "idx_fraud_alerts_risk_level",
                        "idx_fraud_alerts_created_at",
                        "idx_fraud_alerts_customer_date",
                        "idx_fraud_alerts_account_date",
                        "idx_fraud_alerts_risk_level_date",
                        "idx_rule_evaluations_transaction_id",
                        "idx_rule_evaluations_matched");
    }

    private static void insertAuditRows(Connection connection)
            throws SQLException
    {
        executeUpdate(connection, """
                INSERT INTO processed_events (
                    event_id, transaction_id, processing_status, evaluated_at,
                    sanitized_raw_payload, raw_payload_expires_at
                )
                VALUES (
                    'event-1', 'tx-1', 'COMPLETED', '2026-06-07T08:01:00Z',
                    '{"source":"api"}'::jsonb, CURRENT_TIMESTAMP + INTERVAL '7 days'
                )
                """);
        executeUpdate(connection, """
                INSERT INTO transactions (
                    event_id, transaction_id, customer_id, account_id, amount, currency, transaction_timestamp,
                    merchant_category, country, channel, merchant_id, merchant_name, device_id,
                    sanitized_raw_payload, raw_payload_expires_at
                )
                VALUES (
                    'event-1', 'tx-1', 'customer-1', 'account-1', 100.50, 'ZAR', '2026-06-07T08:00:00Z',
                    'GROCERY', 'ZA', 'MOBILE', 'merchant-1', 'Corner Shop', 'device-1',
                    '{"category":"GROCERY"}'::jsonb, CURRENT_TIMESTAMP + INTERVAL '7 days'
                )
                """);
        executeUpdate(connection, """
                INSERT INTO fraud_rules (code, name, description, enabled, severity, score)
                VALUES
                    ('HIGH_AMOUNT', 'High Amount', 'Transaction amount exceeded threshold', TRUE, 'HIGH', 55),
                    ('VELOCITY', 'Velocity', 'Customer stayed below velocity threshold', TRUE, 'MEDIUM', 25)
                """);
        executeUpdate(connection, """
                INSERT INTO rule_evaluations (
                    transaction_id, fraud_rule_id, rule_code, rule_name, matched,
                    score_contribution, explanation, evaluated_at
                )
                VALUES (
                    'tx-1',
                    (SELECT id FROM fraud_rules WHERE code = 'HIGH_AMOUNT'),
                    'HIGH_AMOUNT',
                    'High Amount',
                    TRUE,
                    55,
                    'Amount exceeded configured threshold',
                    '2026-06-07T08:01:00Z'
                )
                """);
        executeUpdate(connection, """
                INSERT INTO rule_evaluations (
                    transaction_id, fraud_rule_id, rule_code, rule_name, matched,
                    score_contribution, explanation, evaluated_at
                )
                VALUES (
                    'tx-1',
                    (SELECT id FROM fraud_rules WHERE code = 'VELOCITY'),
                    'VELOCITY',
                    'Velocity',
                    FALSE,
                    25,
                    'Customer stayed below velocity threshold',
                    '2026-06-07T08:01:00Z'
                )
                """);
        executeUpdate(connection, """
                INSERT INTO fraud_alerts (
                    alert_id, transaction_id, customer_id, account_id, decision, risk_score, risk_level, status
                )
                VALUES (
                    'b3ed20ca-bac6-45dc-a37e-d61001ee37ab',
                    'tx-1',
                    'customer-1',
                    'account-1',
                    'FLAGGED',
                    55,
                    'HIGH',
                    'OPEN'
                )
                """);
    }

    private static void assertDuplicateRaceProtection(Connection connection)
    {
        assertThatThrownBy(() -> executeUpdate(connection, """
                INSERT INTO processed_events (event_id, processing_status)
                VALUES ('event-1', 'DUPLICATE')
                """))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> executeUpdate(connection, """
                INSERT INTO transactions (
                    event_id, transaction_id, customer_id, account_id, amount, currency,
                    transaction_timestamp, merchant_category
                )
                VALUES ('event-2', 'tx-1', 'customer-1', 'account-1', 1.00, 'ZAR', CURRENT_TIMESTAMP, 'GROCERY')
                """))
                .isInstanceOf(SQLException.class);
    }

    private static String alertRetrievalQuery()
    {
        return """
                SELECT COUNT(*)
                FROM fraud_alerts
                WHERE customer_id = 'customer-1'
                  AND account_id = 'account-1'
                  AND risk_level = 'HIGH'
                  AND created_at BETWEEN CURRENT_TIMESTAMP - INTERVAL '1 hour'
                      AND CURRENT_TIMESTAMP + INTERVAL '1 hour'
                """;
    }

    private static Set<String> tableNames(Connection connection)
            throws SQLException
    {
        return queryNames(connection, """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """);
    }

    private static Set<String> columnNames(Connection connection, String tableName)
            throws SQLException
    {
        try (var statement = connection.prepareStatement("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                """))
        {
            statement.setString(1, tableName);
            return readNameSet(statement);
        }
    }

    private static Set<String> constraintNames(Connection connection)
            throws SQLException
    {
        return queryNames(connection, """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                """);
    }

    private static Set<String> indexNames(Connection connection)
            throws SQLException
    {
        return queryNames(connection, """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                """);
    }

    private static Set<String> queryNames(Connection connection, String sql)
            throws SQLException
    {
        try (var statement = connection.prepareStatement(sql))
        {
            return readNameSet(statement);
        }
    }

    private static Set<String> readNameSet(java.sql.PreparedStatement statement)
            throws SQLException
    {
        var names = new HashSet<String>();
        try (var resultSet = statement.executeQuery())
        {
            while (resultSet.next())
            {
                names.add(resultSet.getString(1));
            }
        }

        return names;
    }

    private static int countRows(Connection connection, String sql)
            throws SQLException
    {
        try (var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery())
        {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static void executeUpdate(Connection connection, String sql)
            throws SQLException
    {
        try (var statement = connection.prepareStatement(sql))
        {
            statement.executeUpdate();
        }
    }
}
