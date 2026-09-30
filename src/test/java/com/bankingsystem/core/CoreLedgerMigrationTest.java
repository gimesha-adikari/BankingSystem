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

/**
 * CoreLedgerMigrationTest — Slice 4B-2.1 hardened version.
 *
 * <p>Covers:
 * <ul>
 *   <li>Test 1: Fresh V1→V3 bootstrap + Hibernate schema validation</li>
 *   <li>Test 2: Second Flyway startup is a no-op</li>
 *   <li>Test 3: V2 structures exist; accounts.currency column exists</li>
 *   <li>Test 4: Populated cutover preserves balances; all 7 integrity invariants pass</li>
 *   <li>Test 5: Negative balance blocks V3 cutover (precondition fail-fast)</li>
 *   <li>Test 6: Schema constraints enforce correct data; each failure asserts the
 *               SPECIFIC NAMED CONSTRAINT, not merely any SQL error:
 *     <ul>
 *       <li>XOR identity (chk_ledger_accounts_identity)</li>
 *       <li>Account class (chk_ledger_accounts_class)</li>
 *       <li>Account status (chk_ledger_accounts_status)</li>
 *       <li>Case-sensitive currency on accounts (chk_accounts_currency)</li>
 *       <li>Case-sensitive currency on ledger_accounts (chk_ledger_accounts_currency)</li>
 *       <li>Case-sensitive currency on journal_entries (chk_journal_entries_currency)</li>
 *       <li>Case-sensitive currency on journal_postings (chk_journal_postings_currency)</li>
 *       <li>Journal entry type (chk_journal_entries_type)</li>
 *       <li>Journal status (chk_journal_entries_status)</li>
 *       <li>Positive total_amount (chk_journal_entries_amount)</li>
 *       <li>Actor type (chk_journal_entries_actor_type)</li>
 *       <li>Actor integrity (chk_journal_entries_actor_integrity)</li>
 *       <li>Channel (chk_journal_entries_channel)</li>
 *       <li>Unique entry_reference (uk_journal_entries_reference) — uses two DISTINCT PKs</li>
 *       <li>One reversal per original (uk_journal_entries_reversal_of)</li>
 *       <li>Reversal type integrity CHECK (chk_journal_entries_reversal_integrity)</li>
 *       <li>Posting direction (chk_journal_postings_direction)</li>
 *       <li>Positive posting amount (chk_journal_postings_amount)</li>
 *       <li>Unique (entry_id, sequence_number) (uk_journal_postings_entry_seq)</li>
 *       <li>Idempotency operation type (chk_core_idem_op_type)</li>
 *       <li>Idempotency state (chk_core_idem_state)</li>
 *       <li>Unique (user_id, operation_type, client_key) (uk_core_idem_user_op_key)</li>
 *     </ul>
 *   </li>
 *   <li>Test 7: Mid-cutover failure triggers EXIT HANDLER and rolls back ALL prior DML</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoreLedgerMigrationTest {

    private static final String DB_HOST = envOr("DB_HOST", "127.0.0.1");
    private static final String DB_PORT = envOr("DB_PORT", "3307");
    private static final String DB_USER = envOr("DB_USERNAME", "banking_dev");
    private static final String DB_PASS = envOr("DB_PASSWORD", "change-me-locally");

    private static final String CLEAN_DB          = "banking_ledger_test_clean";
    private static final String CUTOVER_DB        = "banking_ledger_test_cutover";
    private static final String NEGATIVE_DB       = "banking_ledger_test_negative";
    private static final String ATOMIC_DB         = "banking_ledger_test_atomic";
    private static final String REAL_V3_ATOMIC_DB = "banking_ledger_test_real_v3_atomic";

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
            stmt.execute("DROP DATABASE IF EXISTS " + ATOMIC_DB);
            stmt.execute("CREATE DATABASE " + ATOMIC_DB + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            stmt.execute("DROP DATABASE IF EXISTS " + REAL_V3_ATOMIC_DB);
            stmt.execute("CREATE DATABASE " + REAL_V3_ATOMIC_DB + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
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
            stmt.execute("DROP DATABASE IF EXISTS " + ATOMIC_DB);
            stmt.execute("DROP DATABASE IF EXISTS " + REAL_V3_ATOMIC_DB);
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

    // =========================================================================
    // TEST 1: Fresh bootstrap applies all migrations and validates schema
    // =========================================================================
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
        assertThat(result.migrationsExecuted).isEqualTo(4);
        assertThat(result.targetSchemaVersion).isEqualTo("4");

        // Verify Hibernate validates the migrated schema without errors
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        emfBean.setPackagesToScan("com.bankingsystem.core");
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        jpaProperties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        emfBean.setJpaProperties(jpaProperties);
        emfBean.afterPropertiesSet();
        emfBean.destroy();
    }

    // =========================================================================
    // TEST 2: Second Flyway startup is a no-op
    // =========================================================================
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
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
        flyway.validate();
    }

    // =========================================================================
    // TEST 3: V2 structures exist; accounts.currency column defaults to 'LKR'
    // =========================================================================
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

            // All 4 ledger tables exist
            for (String tbl : new String[]{"ledger_accounts", "journal_entries", "journal_postings",
                    "core_transaction_idempotency"}) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + CLEAN_DB
                        + "' AND TABLE_NAME='" + tbl + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Table %s should exist", tbl).isEqualTo(1);
                }
            }

            // V3 system accounts exist
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT system_code, account_class FROM ledger_accounts WHERE system_code IS NOT NULL ORDER BY system_code")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertThat(rs.getString("system_code"))
                            .isIn("SYSTEM_OPENING_BALANCE:LKR", "SYSTEM_VAULT_CASH:LKR");
                    assertThat(rs.getString("account_class")).isEqualTo("ASSET");
                }
                assertThat(count).isEqualTo(2);
            }
        }
    }

    // =========================================================================
    // TEST 4: Populated cutover — all 7 balance integrity invariants
    // =========================================================================
    @Test
    @Order(4)
    void populatedCutoverPreservesBalancesAndBalancesOpeningEntries() throws Exception {
        DataSource ds = createDataSource(CUTOVER_DB);

        // Step A: apply V1 baseline
        Flyway flywayV1 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("1")
                .load();
        flywayV1.migrate();

        // Step B: insert fixtures (Branch, Customer, 4 Accounts with balances 1250.2500 / 0.0000 / 500.0000 / 12.3456)
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
        assertThat(result.migrationsExecuted).isEqualTo(3);

        // Step D: Verify all 7 integrity invariants
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // Invariant 1: Exactly 4 customer ledger accounts (1:1 mapping)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts WHERE customer_account_id IS NOT NULL")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 1: 4 customer ledger accounts").isEqualTo(4);
            }

            // Invariant 2: Exactly 3 opening entries (positive accounts; ACC-002 with 0 has none)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_entries WHERE entry_type = 'OPENING_BALANCE'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 2: 3 opening entries for positive balances").isEqualTo(3);
            }

            // Invariant 3: Every opening entry has exactly 2 balanced postings (DEBIT = CREDIT per entry)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT je.entry_id, COUNT(jp.posting_id) AS cnt, " +
                    "SUM(CASE WHEN jp.direction = 'DEBIT' THEN jp.amount ELSE 0 END) AS debits, " +
                    "SUM(CASE WHEN jp.direction = 'CREDIT' THEN jp.amount ELSE 0 END) AS credits " +
                    "FROM journal_entries je JOIN journal_postings jp ON je.entry_id = jp.entry_id " +
                    "GROUP BY je.entry_id")) {
                int entryCount = 0;
                while (rs.next()) {
                    entryCount++;
                    assertThat(rs.getInt("cnt")).as("Invariant 3: 2 postings per entry").isEqualTo(2);
                    assertThat(rs.getBigDecimal("debits"))
                            .as("Invariant 3: DEBIT = CREDIT within entry")
                            .isEqualByComparingTo(rs.getBigDecimal("credits"));
                }
                assertThat(entryCount).as("Invariant 3: 3 entries had posting pairs").isEqualTo(3);
            }

            // Invariant 4: Derived liability balance matches accounts.balance to 4 decimals
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
                    assertThat(acctBal)
                            .as("Invariant 4: account %s balance matches ledger derived balance", rs.getString("account_number"))
                            .isEqualByComparingTo(derBal);
                    if ("ACC-004".equals(rs.getString("account_number"))) {
                        assertThat(derBal).isEqualByComparingTo(new BigDecimal("12.3456"));
                    }
                }
            }

            // Invariant 5: SYSTEM_OPENING_BALANCE debit total equals total positive account balances (1762.5956)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT la.system_code, " +
                    "COALESCE(SUM(CASE WHEN jp.direction = 'DEBIT' THEN jp.amount ELSE -jp.amount END), 0.0000) AS asset_bal " +
                    "FROM ledger_accounts la LEFT JOIN journal_postings jp ON jp.ledger_account_id = la.ledger_account_id " +
                    "WHERE la.system_code = 'SYSTEM_OPENING_BALANCE:LKR' GROUP BY la.ledger_account_id, la.system_code")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBigDecimal("asset_bal"))
                        .as("Invariant 5: SYSTEM_OPENING_BALANCE = 1762.5956")
                        .isEqualByComparingTo(new BigDecimal("1762.5956"));
            }

            // Invariant 6: SYSTEM_VAULT_CASH has zero postings (not used in opening balance migration)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(jp.posting_id) FROM ledger_accounts la " +
                    "LEFT JOIN journal_postings jp ON jp.ledger_account_id = la.ledger_account_id " +
                    "WHERE la.system_code = 'SYSTEM_VAULT_CASH:LKR'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 6: SYSTEM_VAULT_CASH has 0 postings").isEqualTo(0);
            }

            // Invariant 7: Legacy transactions row count unchanged (2 rows; zero added by migration)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 7: legacy transactions untouched (2 rows)").isEqualTo(2);
            }
        }
    }

    // =========================================================================
    // TEST 5: Negative balance blocks V3 (precondition fail-fast, zero DML)
    // =========================================================================
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

        // Step C: Run V2 + V3 — must fail on V3 precondition A (negative balance)
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
                assertThat(rs.getInt(1)).as("No ledger_accounts written on precondition failure").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_entries")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("No journal_entries written on precondition failure").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_configs WHERE config_key = 'ledger_cutover_at'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("No ledger_cutover_at config written on precondition failure").isEqualTo(0);
            }
        }
    }

    // =========================================================================
    // TEST 6: Schema constraints enforce correct data — each failure asserts the
    //         SPECIFIC NAMED CONSTRAINT name, not merely any SQL error.
    // =========================================================================
    @Test
    @Order(6)
    void schemaConstraintsPreventInvalidData() throws Exception {
        DataSource ds = createDataSource(CLEAN_DB);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");

            // --- ledger_accounts constraints ---

            // chk_ledger_accounts_identity: both customer_account_id and system_code non-null
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0), 'SYS_BOTH', 'ASSET', 'LKR', 'ACTIVE', NOW())"))
                    .as("XOR identity: both non-null")
                    .hasMessageContaining("chk_ledger_accounts_identity");

            // chk_ledger_accounts_identity: both null
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), NULL, NULL, 'ASSET', 'LKR', 'ACTIVE', NOW())"))
                    .as("XOR identity: both null")
                    .hasMessageContaining("chk_ledger_accounts_identity");

            // chk_ledger_accounts_class: invalid account_class
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), NULL, 'SYS-BAD-CLASS', 'REVENUE', 'LKR', 'ACTIVE', NOW())"))
                    .as("Account class: REVENUE not allowed")
                    .hasMessageContaining("chk_ledger_accounts_class");

            // chk_ledger_accounts_status: invalid status
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), NULL, 'SYS-BAD-STATUS', 'ASSET', 'LKR', 'PENDING', NOW())"))
                    .as("Account status: PENDING not allowed")
                    .hasMessageContaining("chk_ledger_accounts_status");

            // chk_accounts_currency: lowercase 'lkr' REJECTED on accounts
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO accounts (account_id, account_number, account_type, account_status, balance, customer_id, branch_id, created_at, updated_at, currency) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'ACC-lkr-cs', 'SAVINGS', 'ACTIVE', 100.0000, UUID_TO_BIN(UUID(), 0), 1, NOW(), NOW(), 'lkr')"))
                    .as("accounts.currency: lowercase 'lkr' must be rejected (case-sensitive)")
                    .hasMessageContaining("chk_accounts_currency");

            // chk_ledger_accounts_currency: lowercase 'lkr' REJECTED on ledger_accounts
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), NULL, 'SYS-lkr-CURR', 'ASSET', 'lkr', 'ACTIVE', NOW())"))
                    .as("ledger_accounts.currency: lowercase 'lkr' must be rejected (case-sensitive)")
                    .hasMessageContaining("chk_ledger_accounts_currency");

            // chk_ledger_accounts_currency: mixed case 'Lkr' REJECTED
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), NULL, 'SYS-Lkr-CURR', 'ASSET', 'Lkr', 'ACTIVE', NOW())"))
                    .as("ledger_accounts.currency: mixed case 'Lkr' must be rejected (case-sensitive)")
                    .hasMessageContaining("chk_ledger_accounts_currency");

            // --- journal_entries constraints ---

            // chk_journal_entries_type: invalid entry_type
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-BAD-TYPE', 'LOAN_PAYMENT', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())"))
                    .as("Journal entry type: LOAN_PAYMENT not allowed")
                    .hasMessageContaining("chk_journal_entries_type");

            // chk_journal_entries_status: invalid status
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-BAD-STATUS', 'DEPOSIT', 'PENDING', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())"))
                    .as("Journal entry status: only POSTED is allowed")
                    .hasMessageContaining("chk_journal_entries_status");

            // chk_journal_entries_amount: zero amount
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-ZERO-AMOUNT', 'DEPOSIT', 'POSTED', 'LKR', 0.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())"))
                    .as("Journal entry amount: zero not allowed")
                    .hasMessageContaining("chk_journal_entries_amount");

            // chk_journal_entries_currency: lowercase 'lkr' on journal_entries
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-lkr-JE', 'DEPOSIT', 'POSTED', 'lkr', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())"))
                    .as("journal_entries.currency: lowercase 'lkr' must be rejected (case-sensitive)")
                    .hasMessageContaining("chk_journal_entries_currency");

            // chk_journal_entries_actor_type: invalid actor_type
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-BAD-ACTOR', 'DEPOSIT', 'POSTED', 'LKR', 100.0000, 'ADMIN', 'TEST', 'SYSTEM', NOW())"))
                    .as("Journal actor type: ADMIN not allowed")
                    .hasMessageContaining("chk_journal_entries_actor_type");

            // chk_journal_entries_actor_integrity: USER type but system_actor_id set (not null)
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, initiated_by_user_id, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-ACTOR-INTEG', 'DEPOSIT', 'POSTED', 'LKR', 100.0000, 'SYSTEM', NULL, NULL, 'SYSTEM', NOW())"))
                    .as("Journal actor integrity: SYSTEM actor cannot have null system_actor_id")
                    .hasMessageContaining("chk_journal_entries_actor_integrity");

            // chk_journal_entries_channel: invalid channel
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-BAD-CHAN', 'DEPOSIT', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'ATM', NOW())"))
                    .as("Journal channel: ATM not allowed (only WEB, MOBILE, TELLER, SYSTEM)")
                    .hasMessageContaining("chk_journal_entries_channel");

            // uk_journal_entries_reference: duplicate entry_reference with DISTINCT PKs
            // First row: unique PK A, unique reference
            stmt.execute("INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN('aa000000-0000-0000-0000-000000000001', 0), 'REF-UNIQUE-SHARED', 'DEPOSIT', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())");
            // Second row: DISTINCT PK B, SAME reference — must fail on uk_journal_entries_reference
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN('aa000000-0000-0000-0000-000000000002', 0), 'REF-UNIQUE-SHARED', 'DEPOSIT', 'POSTED', 'LKR', 200.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())"))
                    .as("Unique entry_reference: second row with different PK but same reference must fail on uk_journal_entries_reference")
                    .hasMessageContaining("uk_journal_entries_reference");

            // chk_journal_entries_reversal_integrity: REVERSAL with null reversal_of_entry_id
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, reversal_of_entry_id, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-BAD-REV-NULL', 'REVERSAL', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', NULL, NOW())"))
                    .as("Reversal integrity: REVERSAL entry cannot have null reversal_of_entry_id")
                    .hasMessageContaining("chk_journal_entries_reversal_integrity");

            // chk_journal_entries_reversal_integrity: DEPOSIT with non-null reversal_of_entry_id
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, reversal_of_entry_id, posted_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), 'REF-BAD-DEP-REV', 'DEPOSIT', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', UUID_TO_BIN(UUID(), 0), NOW())"))
                    .as("Reversal integrity: non-REVERSAL entry cannot have non-null reversal_of_entry_id")
                    .hasMessageContaining("chk_journal_entries_reversal_integrity");

            // uk_journal_entries_reversal_of: second REVERSAL of the same original entry
            // Insert original entry
            stmt.execute("INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, posted_at) " +
                    "VALUES (UUID_TO_BIN('bb000000-0000-0000-0000-000000000001', 0), 'REF-ORIG-FOR-REV', 'DEPOSIT', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', NOW())");
            // First reversal — accepted
            stmt.execute("INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, reversal_of_entry_id, posted_at) " +
                    "VALUES (UUID_TO_BIN('bb000000-0000-0000-0000-000000000002', 0), 'REF-FIRST-REVERSAL', 'REVERSAL', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', UUID_TO_BIN('bb000000-0000-0000-0000-000000000001', 0), NOW())");
            // Second reversal of same original — must fail on uk_journal_entries_reversal_of
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_entries (entry_id, entry_reference, entry_type, status, currency, total_amount, actor_type, system_actor_id, channel, reversal_of_entry_id, posted_at) " +
                    "VALUES (UUID_TO_BIN('bb000000-0000-0000-0000-000000000003', 0), 'REF-SECOND-REVERSAL', 'REVERSAL', 'POSTED', 'LKR', 100.0000, 'SYSTEM', 'TEST', 'SYSTEM', UUID_TO_BIN('bb000000-0000-0000-0000-000000000001', 0), NOW())"))
                    .as("One reversal per original: second reversal of same entry must fail on uk_journal_entries_reversal_of")
                    .hasMessageContaining("uk_journal_entries_reversal_of");

            // --- journal_postings constraints ---

            // Get a valid entry_id and ledger_account_id for posting tests
            String validEntryId = "UUID_TO_BIN('aa000000-0000-0000-0000-000000000001', 0)";
            String validLedgerAccId = "(SELECT ledger_account_id FROM ledger_accounts LIMIT 1)";

            // chk_journal_postings_direction: invalid direction
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_postings (posting_id, entry_id, ledger_account_id, sequence_number, direction, amount, currency, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), " + validEntryId + ", " + validLedgerAccId + ", 99, 'INVALID', 10.0000, 'LKR', NOW())"))
                    .as("Posting direction: INVALID not allowed (only DEBIT, CREDIT)")
                    .hasMessageContaining("chk_journal_postings_direction");

            // chk_journal_postings_amount: zero amount
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_postings (posting_id, entry_id, ledger_account_id, sequence_number, direction, amount, currency, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), " + validEntryId + ", " + validLedgerAccId + ", 98, 'DEBIT', 0.0000, 'LKR', NOW())"))
                    .as("Posting amount: zero not allowed")
                    .hasMessageContaining("chk_journal_postings_amount");

            // chk_journal_postings_currency: lowercase 'lkr' on journal_postings
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_postings (posting_id, entry_id, ledger_account_id, sequence_number, direction, amount, currency, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), " + validEntryId + ", " + validLedgerAccId + ", 97, 'DEBIT', 50.0000, 'lkr', NOW())"))
                    .as("journal_postings.currency: lowercase 'lkr' must be rejected (case-sensitive)")
                    .hasMessageContaining("chk_journal_postings_currency");

            // uk_journal_postings_entry_seq: duplicate (entry_id, sequence_number)
            stmt.execute("INSERT INTO journal_postings (posting_id, entry_id, ledger_account_id, sequence_number, direction, amount, currency, created_at) " +
                    "VALUES (UUID_TO_BIN('cc000000-0000-0000-0000-000000000001', 0), " + validEntryId + ", " + validLedgerAccId + ", 0, 'DEBIT', 100.0000, 'LKR', NOW())");
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO journal_postings (posting_id, entry_id, ledger_account_id, sequence_number, direction, amount, currency, created_at) " +
                    "VALUES (UUID_TO_BIN('cc000000-0000-0000-0000-000000000002', 0), " + validEntryId + ", " + validLedgerAccId + ", 0, 'CREDIT', 100.0000, 'LKR', NOW())"))
                    .as("Unique (entry_id, sequence_number): duplicate pair must fail on uk_journal_postings_entry_seq")
                    .hasMessageContaining("uk_journal_postings_entry_seq");

            // --- core_transaction_idempotency constraints ---

            // chk_core_idem_op_type: invalid operation_type
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO core_transaction_idempotency (id, user_id, operation_type, client_key, request_hash, state, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0), 'LOAN_PAYMENT', 'KEY-1', 'abc123', 'PROCESSING', NOW())"))
                    .as("Idempotency operation type: LOAN_PAYMENT not allowed")
                    .hasMessageContaining("chk_core_idem_op_type");

            // chk_core_idem_state: invalid state
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO core_transaction_idempotency (id, user_id, operation_type, client_key, request_hash, state, created_at) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0), 'DEPOSIT', 'KEY-2', 'abc123', 'PENDING', NOW())"))
                    .as("Idempotency state: PENDING not allowed (only PROCESSING, COMPLETED)")
                    .hasMessageContaining("chk_core_idem_state");

            // uk_core_idem_user_op_key: duplicate (user_id, operation_type, client_key)
            String testUserId = "UUID_TO_BIN('dd000000-0000-0000-0000-000000000001', 0)";
            stmt.execute("INSERT INTO core_transaction_idempotency (id, user_id, operation_type, client_key, request_hash, state, created_at) " +
                    "VALUES (UUID_TO_BIN('dd000000-0000-0000-0000-000000000002', 0), " + testUserId + ", 'DEPOSIT', 'KEY-DEDUP', 'hash1', 'PROCESSING', NOW())");
            assertThatThrownBy(() -> stmt.execute(
                    "INSERT INTO core_transaction_idempotency (id, user_id, operation_type, client_key, request_hash, state, created_at) " +
                    "VALUES (UUID_TO_BIN('dd000000-0000-0000-0000-000000000003', 0), " + testUserId + ", 'DEPOSIT', 'KEY-DEDUP', 'hash2', 'PROCESSING', NOW())"))
                    .as("Unique (user_id, operation_type, client_key): duplicate triplet must fail on uk_core_idem_user_op_key")
                    .hasMessageContaining("uk_core_idem_user_op_key");
        }
    }

    // =========================================================================
    // TEST 7: Mid-cutover DML failure triggers EXIT HANDLER and rolls back ALL
    //         prior DML within the transaction (system_configs + system ledger accounts)
    // =========================================================================
    @Test
    @Order(7)
    void midCutoverFailureRollsBackAllPriorDmlViaExitHandler() throws Exception {
        DataSource ds = createDataSource(ATOMIC_DB);

        // Step A: apply V1 + V2 migrations
        Flyway flywayV2 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("2")
                .load();
        flywayV2.migrate();

        // Step B: insert valid LKR accounts (pass all V3 preconditions)
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO branches (branch_id, address, branch_name, contact_number) " +
                    "VALUES (1, 'Test St', 'Test Branch', '+94112345678')");
            stmt.execute("INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, gender, status, address, created_at, updated_at) " +
                    "VALUES (UUID_TO_BIN('ee000000-0000-0000-0000-000000000001', 0), 'Mid', 'Test', 'mid.test@example.test', '+94771111111', '1990-01-01', 'MALE', 'ACTIVE', 'Test', NOW(), NOW())");
            stmt.execute("INSERT INTO accounts (account_id, account_number, account_status, account_type, balance, branch_id, customer_id, created_at, updated_at) " +
                    "VALUES (UUID_TO_BIN('ee000000-0000-0000-0000-000000000002', 0), 'ACC-ATOMIC', 'ACTIVE', 'SAVINGS', 750.0000, 1, UUID_TO_BIN('ee000000-0000-0000-0000-000000000001', 0), NOW(), NOW())");
        }

        // Step C: Create and call an atomicity-proof procedure that:
        //   - Performs Write 1 (system_configs INSERT) — succeeds
        //   - Performs Write 2 (system ledger_accounts INSERT) — succeeds
        //   - Performs Write 3 (invalid INJECT to trigger chk_ledger_accounts_class) — FAILS
        //   EXIT HANDLER must ROLLBACK all 3 writes.
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // Record before-state
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_configs WHERE config_key='ledger_cutover_at'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Before: no cutover config exists").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Before: no ledger accounts exist").isEqualTo(0);
            }

            // Create the proof procedure
            stmt.execute("DROP PROCEDURE IF EXISTS sp_atomicity_proof");
            stmt.execute(
                "CREATE PROCEDURE sp_atomicity_proof() " +
                "BEGIN " +
                "  DECLARE v_ts DATETIME(6); " +
                "  SET v_ts = UTC_TIMESTAMP(6); " +
                "  BEGIN " +
                "    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END; " +
                "    START TRANSACTION; " +
                "    /* Write 1: system_configs */ " +
                "    INSERT INTO system_configs (config_id, config_key, config_type, config_value, description) " +
                "    VALUES (UUID_TO_BIN(UUID(), 0), 'ledger_cutover_at', 'DATETIME', '2026-01-01T00:00:00Z', 'atomicity proof'); " +
                "    /* Write 2: two system ledger accounts */ " +
                "    INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                "    VALUES " +
                "      (UUID_TO_BIN(UUID(), 0), NULL, 'SYSTEM_VAULT_CASH:LKR', 'ASSET', 'LKR', 'ACTIVE', v_ts), " +
                "      (UUID_TO_BIN(UUID(), 0), NULL, 'SYSTEM_OPENING_BALANCE:LKR', 'ASSET', 'LKR', 'ACTIVE', v_ts); " +
                "    /* Write 3 (INJECTED FAILURE): invalid account_class triggers chk_ledger_accounts_class */ " +
                "    INSERT INTO ledger_accounts (ledger_account_id, customer_account_id, system_code, account_class, currency, status, created_at) " +
                "    VALUES (UUID_TO_BIN(UUID(), 0), NULL, 'SYSTEM_INJECT_FAIL', 'INVALID_CLASS', 'LKR', 'ACTIVE', v_ts); " +
                "    COMMIT; " +
                "  END; " +
                "END"
            );
        }

        // Call the procedure — expect failure
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            assertThatThrownBy(() -> stmt.execute("CALL sp_atomicity_proof()"))
                    .as("Mid-DML failure fires EXIT HANDLER (chk_ledger_accounts_class violated)")
                    .hasMessageContaining("chk_ledger_accounts_class");
        }

        // Step D: Verify ALL DML (Write 1 + Write 2) was rolled back
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_configs WHERE config_key='ledger_cutover_at'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1))
                        .as("Write 1 (system_configs INSERT) rolled back by EXIT HANDLER")
                        .isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1))
                        .as("Write 2 (system ledger_accounts INSERTs) rolled back by EXIT HANDLER")
                        .isEqualTo(0);
            }
        }

        // Step E: Cleanup
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP PROCEDURE IF EXISTS sp_atomicity_proof");
        }
    }

    // =========================================================================
    // TEST 8: Actual V3 migration execution with mid-cutover failure trigger
    //         proves real V3 stored procedure transaction handler rolls back ALL
    //         cutover DML executed prior to the journal_entries failure.
    // =========================================================================
    @Test
    @Order(8)
    void actualV3MidCutoverFailureRollsBackAllPriorV3Dml() throws Exception {
        DataSource ds = createDataSource(REAL_V3_ATOMIC_DB);

        // Step A: apply V1 + V2 using Flyway targeting version 2
        Flyway flywayV2 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("2")
                .load();
        flywayV2.migrate();

        // Step B: insert valid legacy fixtures (Branch, Customer, 2 Accounts, 1 Transaction)
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO branches (branch_id, address, branch_name, contact_number) " +
                    "VALUES (1, 'Main Branch', 'Colombo', '+94112345678')");
            stmt.execute("INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, gender, status, address, created_at, updated_at) " +
                    "VALUES (UUID_TO_BIN('ff000000-0000-0000-0000-000000000001', 0), 'Alice', 'Silva', 'alice.silva@example.test', '+94771234567', '1992-05-15', 'FEMALE', 'ACTIVE', '456 Galle Rd', NOW(), NOW())");
            stmt.execute("INSERT INTO accounts (account_id, account_number, account_status, account_type, balance, branch_id, customer_id, created_at, updated_at, currency) VALUES " +
                    "(UUID_TO_BIN('ff000000-0000-0000-0000-000000000002', 0), 'ACC-REAL-001', 'ACTIVE', 'SAVINGS', 2500.7500, 1, UUID_TO_BIN('ff000000-0000-0000-0000-000000000001', 0), NOW(), NOW(), 'LKR'), " +
                    "(UUID_TO_BIN('ff000000-0000-0000-0000-000000000003', 0), 'ACC-REAL-002', 'ACTIVE', 'CHECKING', 0.0000, 1, UUID_TO_BIN('ff000000-0000-0000-0000-000000000001', 0), NOW(), NOW(), 'LKR')");
            stmt.execute("INSERT INTO transactions (transaction_id, account_id, amount, balance_after, created_at, description, type) " +
                    "VALUES (UUID_TO_BIN(UUID(), 0), UUID_TO_BIN('ff000000-0000-0000-0000-000000000002', 0), 2500.75, 2500.75, NOW(), 'Initial Deposit', 'DEPOSIT')");

            // Step C: Verify all V3 preconditions pass before continuing
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts WHERE balance < 0")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Precondition A: zero negative balances").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts WHERE BINARY currency != 'LKR'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Precondition B: zero non-LKR currencies").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_configs WHERE config_key = 'ledger_cutover_at'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Precondition C: zero cutover marker").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Precondition D1: zero ledger accounts").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_entries")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Precondition D2: zero journal entries").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_postings")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Precondition D3: zero journal postings").isEqualTo(0);
            }

            // Step D: Install test-only failure injector trigger on journal_entries
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_midcutover_fail");
            stmt.execute(
                "CREATE TRIGGER trg_test_midcutover_fail " +
                "BEFORE INSERT ON journal_entries " +
                "FOR EACH ROW " +
                "BEGIN " +
                "  IF NEW.entry_type = 'OPENING_BALANCE' THEN " +
                "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'TEST_ONLY_MIDCUTOVER_FAILURE'; " +
                "  END IF; " +
                "END"
            );
        }

        // Step E: Run the REAL unchanged V3 migration resource through Flyway
        Flyway flywayV3 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("3")
                .load();

        assertThatThrownBy(flywayV3::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("TEST_ONLY_MIDCUTOVER_FAILURE");

        // Step F: Assert every rollback invariant
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Cutover marker
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM system_configs WHERE config_key = 'ledger_cutover_at'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 1: cutover marker rolled back").isEqualTo(0);
            }

            // 2. System ledger accounts
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts WHERE system_code = 'SYSTEM_VAULT_CASH:LKR'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 2a: SYSTEM_VAULT_CASH rolled back").isEqualTo(0);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts WHERE system_code = 'SYSTEM_OPENING_BALANCE:LKR'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 2b: SYSTEM_OPENING_BALANCE rolled back").isEqualTo(0);
            }

            // 3. Customer ledger accounts
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ledger_accounts WHERE customer_account_id IS NOT NULL")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 3: customer ledger accounts rolled back").isEqualTo(0);
            }

            // 4. Journal entries
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_entries")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 4: journal_entries empty").isEqualTo(0);
            }

            // 5. Journal postings
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM journal_postings")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 5: journal_postings empty").isEqualTo(0);
            }

            // 6. Core transaction idempotency
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM core_transaction_idempotency")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 6: idempotency empty").isEqualTo(0);
            }

            // 7. Legacy accounts remain unchanged
            try (ResultSet rs = stmt.executeQuery("SELECT account_number, balance, currency FROM accounts ORDER BY account_number")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("account_number")).isEqualTo("ACC-REAL-001");
                assertThat(rs.getBigDecimal("balance")).isEqualByComparingTo(new BigDecimal("2500.7500"));
                assertThat(rs.getString("currency")).isEqualTo("LKR");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("account_number")).isEqualTo("ACC-REAL-002");
                assertThat(rs.getBigDecimal("balance")).isEqualByComparingTo(new BigDecimal("0.0000"));
                assertThat(rs.getString("currency")).isEqualTo("LKR");
                assertThat(rs.next()).isFalse();
            }

            // 8. Legacy transactions unchanged
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Invariant 8: legacy transactions count unchanged (1 row)").isEqualTo(1);
            }

            // 9. Flyway state: V3 must NOT appear as successful in flyway_schema_history
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND success = 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).as("Flyway: V3 must NOT be marked successful").isEqualTo(0);
            }
        }
    }
}
