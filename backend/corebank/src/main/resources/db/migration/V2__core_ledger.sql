-- ==============================================================================
-- V2__core_ledger.sql
--
-- Slice 4B-2: Core Banking Ledger Schema
--
-- Introduces authoritative financial ledger structures:
-- 1. accounts.currency column (default 'LKR') with ISO-4217 validation.
-- 2. ledger_accounts (supporting both customer liability and system asset/clearing accounts).
-- 3. journal_entries (immutable double-entry event headers).
-- 4. journal_postings (immutable debit/credit leg entries).
-- 5. core_transaction_idempotency (structural idempotency store).
--
-- NOTE:
-- - Zero changes to transactions table in this slice (legacy projection integration is Slice 4B-4).
-- - Zero changes to Account.java or Transaction.java.
-- ==============================================================================

-- 1. Add currency to legacy accounts table
ALTER TABLE `accounts`
  ADD COLUMN `currency` VARCHAR(3) NOT NULL DEFAULT 'LKR',
  ADD CONSTRAINT `chk_accounts_currency` CHECK (`currency` REGEXP '^[A-Z]{3}$');

-- 2. Create ledger_accounts table
CREATE TABLE `ledger_accounts` (
  `ledger_account_id` BINARY(16) NOT NULL,
  `customer_account_id` BINARY(16) DEFAULT NULL,
  `system_code` VARCHAR(50) DEFAULT NULL,
  `account_class` VARCHAR(20) NOT NULL,
  `currency` VARCHAR(3) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`ledger_account_id`),
  UNIQUE KEY `uk_ledger_accounts_customer` (`customer_account_id`),
  UNIQUE KEY `uk_ledger_accounts_system_code` (`system_code`),
  CONSTRAINT `fk_ledger_accounts_customer` FOREIGN KEY (`customer_account_id`) REFERENCES `accounts` (`account_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_ledger_accounts_identity` CHECK (
    (`customer_account_id` IS NOT NULL AND `system_code` IS NULL) OR
    (`customer_account_id` IS NULL AND `system_code` IS NOT NULL)
  ),
  CONSTRAINT `chk_ledger_accounts_class` CHECK (`account_class` IN ('ASSET', 'LIABILITY', 'EQUITY')),
  CONSTRAINT `chk_ledger_accounts_status` CHECK (`status` IN ('ACTIVE', 'FROZEN', 'CLOSED')),
  CONSTRAINT `chk_ledger_accounts_currency` CHECK (`currency` REGEXP '^[A-Z]{3}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Create journal_entries table
CREATE TABLE `journal_entries` (
  `entry_id` BINARY(16) NOT NULL,
  `entry_reference` VARCHAR(64) NOT NULL,
  `entry_type` VARCHAR(30) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'POSTED',
  `currency` VARCHAR(3) NOT NULL,
  `total_amount` DECIMAL(19,4) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `reversal_of_entry_id` BINARY(16) DEFAULT NULL,
  `actor_type` VARCHAR(10) NOT NULL,
  `initiated_by_user_id` BINARY(16) DEFAULT NULL,
  `system_actor_id` VARCHAR(50) DEFAULT NULL,
  `channel` VARCHAR(20) NOT NULL,
  `posted_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`entry_id`),
  UNIQUE KEY `uk_journal_entries_reference` (`entry_reference`),
  UNIQUE KEY `uk_journal_entries_reversal_of` (`reversal_of_entry_id`),
  CONSTRAINT `fk_journal_entries_reversal` FOREIGN KEY (`reversal_of_entry_id`) REFERENCES `journal_entries` (`entry_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_journal_entries_user` FOREIGN KEY (`initiated_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_journal_entries_type` CHECK (`entry_type` IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'REVERSAL', 'OPENING_BALANCE')),
  CONSTRAINT `chk_journal_entries_status` CHECK (`status` = 'POSTED'),
  CONSTRAINT `chk_journal_entries_amount` CHECK (`total_amount` > 0),
  CONSTRAINT `chk_journal_entries_currency` CHECK (`currency` REGEXP '^[A-Z]{3}$'),
  CONSTRAINT `chk_journal_entries_actor_type` CHECK (`actor_type` IN ('USER', 'SYSTEM')),
  CONSTRAINT `chk_journal_entries_actor_integrity` CHECK (
    (`actor_type` = 'USER' AND `initiated_by_user_id` IS NOT NULL AND `system_actor_id` IS NULL) OR
    (`actor_type` = 'SYSTEM' AND `system_actor_id` IS NOT NULL AND `initiated_by_user_id` IS NULL)
  ),
  CONSTRAINT `chk_journal_entries_channel` CHECK (`channel` IN ('WEB', 'MOBILE', 'TELLER', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Create journal_postings table
CREATE TABLE `journal_postings` (
  `posting_id` BINARY(16) NOT NULL,
  `entry_id` BINARY(16) NOT NULL,
  `ledger_account_id` BINARY(16) NOT NULL,
  `sequence_number` INT NOT NULL,
  `direction` VARCHAR(10) NOT NULL,
  `amount` DECIMAL(19,4) NOT NULL,
  `currency` VARCHAR(3) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`posting_id`),
  UNIQUE KEY `uk_journal_postings_entry_seq` (`entry_id`, `sequence_number`),
  KEY `idx_journal_postings_account_created` (`ledger_account_id`, `created_at`),
  CONSTRAINT `fk_journal_postings_entry` FOREIGN KEY (`entry_id`) REFERENCES `journal_entries` (`entry_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_journal_postings_account` FOREIGN KEY (`ledger_account_id`) REFERENCES `ledger_accounts` (`ledger_account_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_journal_postings_direction` CHECK (`direction` IN ('DEBIT', 'CREDIT')),
  CONSTRAINT `chk_journal_postings_amount` CHECK (`amount` > 0),
  CONSTRAINT `chk_journal_postings_currency` CHECK (`currency` REGEXP '^[A-Z]{3}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Create core_transaction_idempotency table
CREATE TABLE `core_transaction_idempotency` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `operation_type` VARCHAR(30) NOT NULL,
  `client_key` VARCHAR(128) NOT NULL,
  `request_hash` VARCHAR(64) NOT NULL,
  `state` VARCHAR(20) NOT NULL,
  `entry_id` BINARY(16) DEFAULT NULL,
  `response_status_code` INT DEFAULT NULL,
  `response_payload` TEXT DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `completed_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_core_idem_user_op_key` (`user_id`, `operation_type`, `client_key`),
  CONSTRAINT `fk_core_idem_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_idem_entry` FOREIGN KEY (`entry_id`) REFERENCES `journal_entries` (`entry_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_core_idem_op_type` CHECK (`operation_type` IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
  CONSTRAINT `chk_core_idem_state` CHECK (`state` IN ('PROCESSING', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
