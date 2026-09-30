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

---

## 2. Operational Procedures

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

Use the provided operator scripts located in `scripts/db/`:

#### Step 1: Verify Schema Compatibility
Run the verification tool to ensure the target database has not drifted from the canonical baseline:
```bash
./scripts/db/verify-pre-flyway-schema.sh \
  --host 127.0.0.1 \
  --port 3307 \
  --user banking_dev \
  --password change-me-locally \
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
  --password change-me-locally \
  --database banking_system_dev \
  --version 1
```
This script:
1. Automatically re-runs `verify-pre-flyway-schema.sh`. If verification fails, it aborts immediately.
2. Creates `flyway_schema_history`.
3. Inserts an entry recording version `1` as `BASELINE` (`success: 1`).

#### Step 3: Start Application
Start the application normally. Flyway will recognise version `1` as current, skip re-executing `V1__baseline.sql`, and Hibernate will validate the schema.

---

## 3. Adding New Migrations

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
   - Include `FOREIGN_KEY_CHECKS` toggles if table drops or reorders occur.
   - Never modify an already-committed migration file. If a change is needed, add `V<next>__...`.

---

## 4. Troubleshooting & FAQ

### Q: Startup fails with `Found non-empty schema(s) ... but no schema history table`
**Cause:** The target database contains tables, but Flyway has not been initialized.
**Remedy:** Do NOT drop the database if it contains real data. Follow Section 2B ("Adopting an Existing Pre-Flyway Database").

### Q: Startup fails with `SchemaManagementException: Schema-validation: missing column [...]`
**Cause:** An entity has an `@Column` or relationship that does not exist in the database.
**Remedy:** Ensure all migrations have run. If the entity was modified, create a new Flyway migration to alter the table accordingly.

### Q: Startup fails with `FlywayValidateException: Validate failed: Migration checksum mismatch`
**Cause:** An existing, already-applied migration file in `db/migration/` was modified locally.
**Remedy:** Never alter applied migration scripts. Revert changes to the committed script and create a forward migration instead.
