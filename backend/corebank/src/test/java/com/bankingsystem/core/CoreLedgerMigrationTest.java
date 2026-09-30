package com.bankingsystem.core;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoreLedgerMigrationTest {

    private static final String DB_HOST = envOr("DB_HOST", "127.0.0.1");
    private static final String DB_PORT = envOr("DB_PORT", "3307");
    private static final String DB_USER = envOr("DB_USERNAME", "banking_dev");
    private static final String DB_PASS = envOr("DB_PASSWORD", "change-me-locally");

    private static final String CLEAN_DB = "banking_ledger_test_clean";
    private static final String CUTOVER_DB = "banking_ledger_test_cutover";
    private static final String NEGATIVE_DB = "banking_ledger_test_negative";

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
        assumeTrue(isDatabaseAvailable(), "MySQL is not reachable. Skipping CoreLedgerMigrationTest.");
        try (Connection c = DriverManager.getConnection(getBaseJdbcUrl(), DB_USER, DB_PASS);
             Statement stmt = c.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CLEAN_DB);
            stmt.execute("CREATE DATABASE " + CLEAN_DB + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            stmt.execute("DROP DATABASE IF EXISTS " + CUTOVER_DB);
            stmt.execute("CREATE DATABASE " + CUTOVER_DB + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            stmt.execute("DROP DATABASE IF EXISTS " + NEGATIVE_DB);
            stmt.execute("CREATE DATABASE " + NEGATIVE_DB + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
    }

    @AfterAll
    static void cleanup() {
        if (!isDatabaseAvailable()) return;
        try (Connection c = DriverManager.getConnection(getBaseJdbcUrl(), DB_USER, DB_PASS);
             Statement stmt = c.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CLEAN_DB);
            stmt.execute("DROP DATABASE IF EXISTS " + CUTOVER_DB);
            stmt.execute("DROP DATABASE IF EXISTS " + NEGATIVE_DB);
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
    void freshBootstrapAppliesAllMigrationsAndValidatesSchema() {
        DataSource ds = createDataSource(CLEAN_DB);
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(3);
        assertThat(result.targetSchemaVersion).isEqualTo("3");

        // Verify Hibernate validates schema cleanly
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        emfBean.setPackagesToScan("com.bankingsystem.core");
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        jpaProperties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        emfBean.setJpaProperties(jpaProperties);
        emfBean.afterPropertiesSet();
        emfBean.destroy();
    }

    @Test
    @Order(2)
    void secondFlywayStartupIsNoOp() {
        DataSource ds = createDataSource(CLEAN_DB);
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(0);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        flyway.validate();
    }

    @Test
    @Order(3)
    void v2SchemaStructuresAndAccountsCurrencyExist() throws Exception {
        DataSource ds = createDataSource(CLEAN_DB);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // accounts.currency exists with default 'LKR'
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COLUMN_DEFAULT, IS_NULLABLE FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA='" + CLEAN_DB + "' AND TABLE_NAME='accounts' AND COLUMN_NAME='currency'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("COLUMN_DEFAULT")).contains("LKR");
                assertThat(rs.getString("IS_NULLABLE")).isEqualTo("NO");
            }

            // Ledger tables exist
            for (String tbl : new String[]{"ledger_accounts", "journal_entries", "journal_postings", "core_transaction_idempotency"}) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + CLEAN_DB + "' AND TABLE_NAME='" + tbl + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }

            // System accounts exist
            try (ResultSet rs = stmt.executeQuery("SELECT system_code, account_class FROM ledger_accounts WHERE system_code IS NOT NULL")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertThat(rs.getString("system_code")).isIn("SYSTEM_VAULT_CASH:LKR", "SYSTEM_OPENING_BALANCE:LKR");
                    assertThat(rs.getString("account_class")).isEqualTo("ASSET");
                }
                assertThat(count).isEqualTo(2);
            }
        }
    }

    @Test
    @Order(4)
    void populatedCutoverPreservesBalancesAndBalancesOpeningEntries() throws Exception {
        DataSource ds = createDataSource(CUTOVER_DB);

        // Step A: apply V1 baseline and baseline at 1
        Flyway flywayV1 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("1")
                .load();
        flywayV1.migrate();

        // Step B: insert fixtures (Branch, Customer, 4 Accounts, 2 Transactions)
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO branches (branch_id, address, branch_name, contact_number) " +
                    "VALUES (1, 'Main St', 'Main Branch', '+94112345678')");
            stmt.execute("INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, gender, status, address, created_at, updated_at) " +
                    "VALUES (UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 0), 'John', 'Doe', 'cutover.test@example.test', '+94771234567', '1990-01-01', 'MALE', 'ACTIVE', '123 Main', NOW(), NOW())");
            stmt.execute("INSERT INTO accounts (account_id, account_number, account_status, account_type, balance, branch_id, customer_id, created_at, updated_at) VALUES " +
                    "(UUID_TO_BIN('11111111-1111-1111-1111-111111111111', 0), 'ACC-001', 'ACTIVE', 'SAVINGS', 1250.2500, 1, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 0), NOW(), NOW()), " +
                    "(UUID_TO_BIN('22222222-2222-2222-2222-222222222222', 0), 'ACC-002', 'ACTIVE', 'CHECKING', 0.0000, 1, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 0), NOW(), NOW()), " +
                    "(UUID_TO_BIN('33333333-3333-3333-3333-333333333333', 0), 'ACC-003', 'FROZEN', 'SAVINGS', 500.0000, 1, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 0), NOW(), NOW()), " +
                    "(UUID_TO_BIN('44444444-4444-4444-4444-444444444444', 0), 'ACC-004', 'ACTIVE', 'SAVINGS', 12.3456, 1, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 0), NOW(), NOW())");
            stmt.execute("INSERT INTO transactions (transaction_id, account_id, amount, balance_after, created_at, description, type) VALUES " +
                    "(UUID_TO_BIN(UUID(), 0), UUID_TO_BIN('11111111-1111-1111-1111-111111111111', 0), 1250.25, 1250.25, NOW(), 'Dep', 'DEPOSIT'), " +
                    "(UUID_TO_BIN(UUID(), 0), UUID_TO_BIN('33333333-3333-3333-3333-333333333333', 0), 500.00, 500.00, NOW(), 'Dep', 'DEPOSIT')");
        }

        // Step C: Run V2 and V3
        Flyway flywayFull = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();
        MigrateResult result = flywayFull.migrate();
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(2);

        // Step D: Verify Cutover Integrity
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Exactly 4 customer ledger accounts (1:1 mapping)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts WHERE customer_account_id IS NOT NULL")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(4);
            }

            // 2. Exactly 3 opening entries (positive accounts; ACC-002 with 0 has none)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_entries WHERE entry_type = 'OPENING_BALANCE'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(3);
            }

            // 3. Every opening entry has exactly 2 balanced postings
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT je.entry_id, COUNT(jp.posting_id) AS cnt, " +
                    "SUM(CASE WHEN jp.direction = 'DEBIT' THEN jp.amount ELSE 0 END) AS debits, " +
                    "SUM(CASE WHEN jp.direction = 'CREDIT' THEN jp.amount ELSE 0 END) AS credits " +
                    "FROM journal_entries je JOIN journal_postings jp ON je.entry_id = jp.entry_id " +
                    "GROUP BY je.entry_id")) {
                int entryCount = 0;
                while (rs.next()) {
                    entryCount++;
                    assertThat(rs.getInt("cnt")).isEqualTo(2);
                    assertThat(rs.getBigDecimal("debits")).isEqualByComparingTo(rs.getBigDecimal("credits"));
                }
                assertThat(entryCount).isEqualTo(3);
            }

            // 4. Derived liability balance matches accounts.balance to 4 decimals
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT a.account_number, a.balance, " +
                    "COALESCE(SUM(CASE WHEN jp.direction = 'CREDIT' THEN jp.amount ELSE -jp.amount END), 0.0000) AS derived " +
                    "FROM accounts a " +
                    "JOIN ledger_accounts la ON la.customer_account_id = a.account_id " +
                    "LEFT JOIN journal_postings jp ON jp.ledger_account_id = la.ledger_account_id " +
                    "GROUP BY a.account_id, a.account_number, a.balance")) {
                while (rs.next()) {
                    BigDecimal acctBal = rs.getBigDecimal("balance");
                    BigDecimal derBal = rs.getBigDecimal("derived");
                    assertThat(acctBal).isEqualByComparingTo(derBal);
                    if ("ACC-004".equals(rs.getString("account_number"))) {
                        assertThat(derBal).isEqualByComparingTo(new BigDecimal("12.3456"));
                    }
                }
            }

            // 5. System accounts: SYSTEM_OPENING_BALANCE equals total positive balances (1762.5956)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT la.system_code, " +
                    "COALESCE(SUM(CASE WHEN jp.direction = 'DEBIT' THEN jp.amount ELSE -jp.amount END), 0.0000) AS asset_bal " +
                    "FROM ledger_accounts la LEFT JOIN journal_postings jp ON jp.ledger_account_id = la.ledger_account_id " +
                    "WHERE la.system_code = 'SYSTEM_OPENING_BALANCE:LKR' GROUP BY la.ledger_account_id, la.system_code")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBigDecimal("asset_bal")).isEqualByComparingTo(new BigDecimal("1762.5956"));
            }

            // 6. SYSTEM_VAULT_CASH has 0 postings
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(jp.posting_id) FROM ledger_accounts la " +
                    "LEFT JOIN journal_postings jp ON jp.ledger_account_id = la.ledger_account_id " +
                    "WHERE la.system_code = 'SYSTEM_VAULT_CASH:LKR'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }

            // 7. Legacy transactions row count unchanged (2 rows; zero added)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    @Test
    @Order(5)
    void negativeBalanceBlocksCutoverBeforeDataChanges() throws Exception {
        DataSource ds = createDataSource(NEGATIVE_DB);

        // Step A: apply V1
        Flyway flywayV1 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("1")
                .load();
        flywayV1.migrate();

        // Step B: insert negative balance account
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO branches (branch_id, address, branch_name, contact_number) " +
                    "VALUES (1, 'Main St', 'Main Branch', '+94112345678')");
            stmt.execute("INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, gender, status, address, created_at, updated_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'Neg', 'User', 'neg.user@example.test', '+94771234569', '1990-01-01', 'MALE', 'ACTIVE', '123 Main', NOW(), NOW())");
            stmt.execute("INSERT INTO accounts (account_id, account_number, account_status, account_type, balance, branch_id, customer_id, created_at, updated_at) " +
                    "SELECT UUID_TO_BIN(UUID(), 0), 'ACC-NEG', 'ACTIVE', 'SAVINGS', -100.0000, 1, customer_id, NOW(), NOW() FROM customers LIMIT 1");
        }

        // Step C: Run Flyway to latest -> must fail on V3
        Flyway flywayFull = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        assertThatThrownBy(flywayFull::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Negative account balance detected");

        // Step D: Verify fail-fast: zero ledger accounts, zero journal entries, zero cutover config
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_entries")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_configs WHERE config_key = 'ledger_cutover_at'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    @Test
    @Order(6)
    void schemaConstraintsPreventInvalidData() throws Exception {
        DataSource ds = createDataSource(CLEAN_DB);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. XOR check: both non-null
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0), 'SYS_BOTH', 'ASSET', 'LKR', 'ACTIVE', NOW())"))
                    .hasMessageContaining("chk_ledger_accounts_identity");

            // 2. XOR check: both null
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), NULL, NULL, 'ASSET', 'LKR', 'ACTIVE', NOW())"))
                    .hasMessageContaining("chk_ledger_accounts_identity");

            // 3. Journal entry amount must be positive
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-ZERO-TEST', 'DEPOSIT', 'POSTED', 'LKR', 0.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())"))
                    .hasMessageContaining("chk_journal_entries_amount");

            // 4. Journal posting direction must be DEBIT or CREDIT
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_postings (posting_id, entry_id, ledger_account_id, sequence_number, direction, amount, currency, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0), 0, 'INVALID', 10.0000, 'LKR', NOW())"))
                    .hasMessageContaining("chk_journal_postings_direction");
        }
    }
}
