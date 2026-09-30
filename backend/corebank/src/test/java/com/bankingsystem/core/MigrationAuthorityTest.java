package com.bankingsystem.core;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MigrationAuthorityTest {

    private static final String DB_HOST = envOr("DB_HOST", "127.0.0.1");
    private static final String DB_PORT = envOr("DB_PORT", "3307");
    private static final String DB_USER = envOr("DB_USERNAME", "banking_dev");
    private static final String DB_PASS = envOr("DB_PASSWORD", "change-me-locally");

    private static final String CLEAN_DB_NAME = "banking_flyway_test_clean";
    private static final String MISMATCH_DB_NAME = "banking_flyway_test_mismatch";

    private static String getJdbcUrl(String dbName) {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + dbName
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String getBaseJdbcUrl() {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String envOr(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private static boolean isDatabaseAvailable() {
        try (Connection c = DriverManager.getConnection(getBaseJdbcUrl(), DB_USER, DB_PASS)) {
            return c != null && !c.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void checkDatabaseAndSetup() throws Exception {
        assumeTrue(isDatabaseAvailable(), "MySQL on " + DB_HOST + ":" + DB_PORT + " is not reachable. Skipping migration authority tests.");
        try (Connection c = DriverManager.getConnection(getBaseJdbcUrl(), DB_USER, DB_PASS);
             Statement stmt = c.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CLEAN_DB_NAME);
            stmt.execute("CREATE DATABASE " + CLEAN_DB_NAME);
            stmt.execute("DROP DATABASE IF EXISTS " + MISMATCH_DB_NAME);
            stmt.execute("CREATE DATABASE " + MISMATCH_DB_NAME);
        }
    }

    @AfterAll
    static void cleanup() {
        if (!isDatabaseAvailable()) return;
        try (Connection c = DriverManager.getConnection(getBaseJdbcUrl(), DB_USER, DB_PASS);
             Statement stmt = c.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CLEAN_DB_NAME);
            stmt.execute("DROP DATABASE IF EXISTS " + MISMATCH_DB_NAME);
        } catch (Exception ignored) {
        }
    }

    private DataSource createDataSource(String dbName) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(getJdbcUrl(dbName));
        ds.setUsername(DB_USER);
        ds.setPassword(DB_PASS);
        return ds;
    }

    @Test
    @Order(1)
    void cleanDatabaseBootstrapsThroughFlyway() throws Exception {
        DataSource ds = createDataSource(CLEAN_DB_NAME);
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(3);
        assertThat(result.targetSchemaVersion).isEqualTo("3");

        // Verify 35 domain tables exist in clean DB (31 baseline + 4 ledger tables)
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT count(*) FROM information_schema.tables WHERE table_schema='" + CLEAN_DB_NAME + "' AND table_name != 'flyway_schema_history'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(35);
        }
    }

    @Test
    @Order(2)
    void flywayMigrationIsIdempotentAcrossRestarts() {
        DataSource ds = createDataSource(CLEAN_DB_NAME);
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        // Second run on already migrated clean DB
        MigrateResult secondResult = flyway.migrate();

        assertThat(secondResult.success).isTrue();
        assertThat(secondResult.migrationsExecuted).isEqualTo(0);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");

        // Validate should pass cleanly without throwing exception
        flyway.validate();
    }

    @Test
    @Order(3)
    void hibernateValidationFailsOnSchemaMismatch() throws Exception {
        DataSource ds = createDataSource(MISMATCH_DB_NAME);

        // First apply V1 baseline so schema exists
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        // Introduce deliberate schema mismatch: drop required column 'balance' from 'accounts'
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE accounts DROP COLUMN balance");
        }

        // Configure Spring/Hibernate EntityManagerFactory with ddl-auto: validate
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        emfBean.setPackagesToScan("com.bankingsystem.core");
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        emfBean.setJpaProperties(jpaProperties);

        // Attempt initialization: must fail due to schema validation mismatch
        assertThatThrownBy(emfBean::afterPropertiesSet)
                .satisfies(throwable -> {
                    String message = throwable.getMessage();
                    Throwable cause = throwable.getCause();
                    while (cause != null) {
                        message += " | " + cause.getMessage();
                        cause = cause.getCause();
                    }
                    assertThat(message).contains("missing column [balance] in table [accounts]");
                });
    }

    @Test
    @Order(4)
    void freshBootstrapDoesNotPreloadRuntimeAutoIncrementCounters() throws Exception {
        DataSource ds = createDataSource(CLEAN_DB_NAME);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT AUTO_INCREMENT FROM information_schema.tables WHERE table_schema='" + CLEAN_DB_NAME + "' AND table_name='branches'")) {
            assertThat(rs.next()).isTrue();
            long autoInc = rs.getLong(1);
            // In MySQL, before rows are inserted, AUTO_INCREMENT is 1 (or NULL/0), never 4
            assertThat(autoInc).isLessThanOrEqualTo(1L);
        }
    }
}
