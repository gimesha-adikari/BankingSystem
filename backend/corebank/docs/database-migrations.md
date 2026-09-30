# Database Migration Management Guide

## 1. Overview & Architecture

As of Slice 4B-1, **Flyway** is the sole authoritative mechanism for managing database schema evolution in `BankingSystem`. 

Hibernate DDL generation (`ddl-auto: update`) has been permanently decommissioned in favor of:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: false
    locations: classpath:db/migration
```

### Core Invariants:
1. **Migration Authority:** No table, index, column, constraint, or trigger may be created by JPA/Hibernate or ad-hoc manual SQL. All schema changes must be versioned Flyway migration files (`V<version>__<description>.sql`).
2. **Fail-Fast Validation:** On startup, Hibernate validates entity mappings against the database schema. Any missing table, missing column, or incompatible type immediately halts application startup.
3. **Explicit Baselines Only:** `spring.flyway.baseline-on-migrate` is set to `false`. Flyway will refuse to run on unmanaged non-empty databases unless explicitly verified and baselined by an operator.
4. **Zero Domain Logic in Migrations:** Migrations must contain only DDL. Business seed data (e.g. system roles, initial admin accounts) is managed by idempotent Spring beans (`CommandLineRunner` / `DefaultUsersInitializer`).
5. **No Runtime Auto-Increment Counters in Baselines:** Table options like `AUTO_INCREMENT=N` must never be hardcoded into migration DDL. Sequence counters are runtime data state, not schema definitions.

---

## 2. Schema Equivalence Standards

We distinguish between two distinct levels of schema equivalence:

### A. Structural Schema Equivalence
- **Definition:** Compares table structures, column definitions, data types, nullability, unique keys, foreign key constraints, and secondary indexes while ignoring runtime sequence counters (`AUTO_INCREMENT=N`).
- **Use Case:** Used by `verify-pre-flyway-schema.sh` when checking whether an existing populated database is safe to bring under Flyway version 1 baseline authority.

### B. Fresh Bootstrap Equivalence
- **Definition:** Compares clean database initialization from zero, verifying that:
  1. The DDL applies cleanly without hardcoded sequence counters.
  2. Hibernate validates all 31 entity definitions against the newly generated tables.
  3. Default seeders execute idempotently and generate standard primary keys (e.g., initial branch IDs `1, 2, 3`).
  4. Subsequent boots execute 0 migrations with 0 schema modifications.
- **Use Case:** Validates that new developer environments, automated CI test containers, and disaster-recovery rebuilds behave identically to fresh legacy environments.

---

## 3. Operational Procedures

### A. Bootstrapping a New (Empty) Database
For fresh local development, test environments, or newly provisioned cloud instances:
1. Ensure MySQL 8.4+ is running.
2. Create an empty database:
   ```sql
   CREATE DATABASE banking_system_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
3. Start the application:
   ```bash
   ./gradlew bootRun
   ```
   **What happens automatically:**
   - Flyway detects an empty database.
   - Flyway creates the `flyway_schema_history` table.
   - Flyway applies `V1__baseline.sql` (creating all 31 baseline domain tables).
   - Hibernate runs in `validate` mode and confirms all entities match the schema.
   - Seeders execute idempotently to initialize roles and bootstrap accounts.

---

### B. Adopting an Existing Pre-Flyway Database
If upgrading a database that was previously managed under Hibernate `ddl-auto: update`:

> [!WARNING]
> DO NOT enable `baseline-on-migrate: true` in application configuration. Doing so will bypass drift verification and could record a corrupted baseline.
> DO NOT pass database passwords as command-line arguments. Plaintext credentials in process arguments are prohibited.

Use the provided operator scripts located in `scripts/db/`:

#### Step 1: Verify Schema Compatibility
Run the verification tool using a credentials file or environment variable:
```bash
# Option 1: Using a protected credentials file (mode 0600)
./scripts/db/verify-pre-flyway-schema.sh \
  --host 127.0.0.1 \
  --port 3307 \
  --user banking_dev \
  --defaults-file /path/to/protected-creds.cnf \
  --database banking_system_dev

# Option 2: Using environment variable
export DB_PASSWORD="change-me-locally"
./scripts/db/verify-pre-flyway-schema.sh \
  --host 127.0.0.1 \
  --port 3307 \
  --user banking_dev \
  --database banking_system_dev
```
- If the schema matches: exits `0` with `VERDICT: DATABASE IS SAFE TO BASELINE AT VERSION 1.`
- If drift is detected: exits `1` with exact table/column/constraint differences. Fix drift before proceeding.

#### Step 2: Apply the Explicit Baseline Marker
Run the baselining script:
```bash
./scripts/db/baseline-existing-database.sh \
  --host 127.0.0.1 \
  --port 3307 \
  --user banking_dev \
  --database banking_system_dev \
  --version 1
```
This script:
1. Re-runs `verify-pre-flyway-schema.sh`. If verification fails, it aborts immediately.
2. Invokes Flyway's official Java API (`Flyway.configure().baseline()`) via `FlywayBaselineOperator` to establish the baseline.
3. Does **not** handcraft or insert raw SQL into `flyway_schema_history`.

#### Step 3: Start Application
Start the application normally. Flyway will recognise version `1` as current, skip re-executing `V1__baseline.sql`, and Hibernate will validate the schema.

---

## 4. Database Engine Compatibility Note (MySQL 8.4)
- **Status:** **VERIFIED FUNCTIONAL IN THIS AUDIT — UPSTREAM VERSION WARNING PRESENT**
- **Details:** Flyway 10.20.1 emits an informational recommendation on startup:
  `WARN o.f.c.i.database.base.Database - Flyway upgrade recommended: MySQL 8.4 is newer than this version of Flyway and support has not been tested. The latest supported version of MySQL is 8.1.`
- **Operational Reality:** All Flyway 10.20.1 commands (history table creation, baseline recording, schema migration, and checksum validation) execute with 100% functional reliability against MySQL 8.4.11.

---

## 5. Adding New Migrations

When introducing schema modifications in future slices (e.g. Slice 4B-2 Ledger Schema):
1. Create a new SQL file in `backend/corebank/src/main/resources/db/migration/`:
   ```
   V2__core_banking_ledger.sql
   ```
2. Adhere strictly to Flyway file naming:
   - Prefix: `V` (versioned)
   - Version number: Integer or dotted (e.g., `2`, `2.1`, `3`)
   - Separator: `__` (two underscores)
   - Description: lowercase with underscores (e.g., `core_banking_ledger`)
   - Suffix: `.sql`
3. SQL Standards:
   - Use `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;` for all tables.
   - Use backticks around identifiers.
   - Do NOT include `AUTO_INCREMENT=N` table options.
   - Include `FOREIGN_KEY_CHECKS` toggles if table drops or reorders occur.
   - Never modify an already-committed migration file. If a change is needed, add `V<next>__...`.

---

## 6. Migration Registry

### V1 — Canonical Pre-Ledger Baseline
- **File:** `V1__baseline.sql`
- **Scope:** 31 domain tables representing the hardened pre-ledger schema as of Slice 4B-1.
- **Invariants:** Clean zero-state schema with no runtime `AUTO_INCREMENT` state.

### V2 — Core Banking Ledger Schema
- **File:** `V2__core_ledger.sql`
- **Scope:**
  1. `accounts.currency`: Compatibility column `VARCHAR(3) NOT NULL DEFAULT 'LKR'` with case-sensitive format validation (`CHECK (REGEXP_LIKE(currency, '^[A-Z]{3}$', 'c'))`). Validates format only (e.g., 'ZZZ' structurally passes; 'lkr' is rejected); ISO-4217 membership validation is enforced at application layer.
  2. `ledger_accounts`: Supporting both customer liability accounts and system asset/clearing accounts with XOR identity constraint (`chk_ledger_accounts_identity`), classes (`ASSET`, `LIABILITY`, `EQUITY`), statuses (`ACTIVE`, `FROZEN`, `CLOSED`), and case-sensitive currency check.
  3. `journal_entries`: Immutable double-entry transaction event headers with unique `entry_reference`, reversal self-reference FK with unique constraint (1:1 reversal invariant), reversal structural integrity constraint (`chk_journal_entries_reversal_integrity` enforcing that only `REVERSAL` entries may reference another entry), actor polymorphism check (`USER` vs `SYSTEM`), and channel check.
  4. `journal_postings`: Immutable debit/credit leg entries with foreign keys to entry and ledger account, sequence number uniqueness per entry, positive amount check, and composite index on `(ledger_account_id, created_at)`.
  5. `core_transaction_idempotency`: Structural idempotency store scoped to `(user_id, operation_type, client_key)` with lifecycle states (`PROCESSING`, `COMPLETED`).
- **Invariants:**
  - Journal tables are designed for append-only use; no update timestamp or mutation workflow exists. Application-level immutability will be enforced in later ledger code, with reconciliation as defense in depth.
  - Zero modifications to the legacy `transactions` table in this slice (legacy projection integration is deferred to Slice 4B-4).
  - NO business posting engine, deposit/withdrawal/transfer endpoints, or JPA ledger entities exist yet.

### V3 — Opening Balance Migration & Ledger Cutover
- **File:** `V3__opening_balance_migration.sql`
- **Scope:**
  1. Fail-fast validation of legacy preconditions via stored procedure `sp_migrate_opening_balances()`:
     - Negative balances (`accounts.balance < 0`) immediately abort the migration (`SIGNAL SQLSTATE '45000'`).
     - Non-LKR account currencies abort the migration using case-sensitive binary comparison (`BINARY currency != 'LKR'`).
     - Pre-existing cutover markers (`system_configs.config_key = 'ledger_cutover_at'`) or partial ledger rows abort the migration.
  2. Explicit transaction atomicity: all DML is wrapped in an explicit `START TRANSACTION` / `COMMIT` block with a `DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END` ensuring that any mid-procedure error rolls back all prior cutover DML completely.
  3. Explicit single deterministic UTC cutover timestamp captured via `UTC_TIMESTAMP(6)`.
  4. Cutover timestamp recorded in `system_configs` under `ledger_cutover_at` (`config_type = 'DATETIME'`).
  5. System accounts initialized:
     - `SYSTEM_VAULT_CASH:LKR` (ASSET, 0 postings initially).
     - `SYSTEM_OPENING_BALANCE:LKR` (ASSET, holds migration clearing debit balance).
  6. Customer liability ledger accounts created 1:1 for every account in `accounts`, matching account status.
  7. Opening-balance accounting:
     - For accounts with `balance > 0`, an `OPENING_BALANCE` journal entry is created with reference `CUTOVER-<hex(account_id)>`, actor `SYSTEM`, and system actor ID `MIGRATION_CUTOVER`.
     - Exactly two postings per entry: Posting 0 (DEBIT `SYSTEM_OPENING_BALANCE:LKR`) and Posting 1 (CREDIT customer liability account).
     - Zero-balance accounts (`balance = 0.0000`) receive customer ledger accounts but ZERO opening journal entries and postings.
     - Four-decimal precision is preserved exactly (e.g. `12.3456`).
  8. Non-destructive invariants:
     - Zero modifications to `accounts.balance` (values remain exact).
     - Zero modifications to legacy `transactions` history (no synthetic rows added).

