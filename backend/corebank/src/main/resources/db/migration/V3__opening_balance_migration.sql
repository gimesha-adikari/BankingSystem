-- ==============================================================================
-- V3__opening_balance_migration.sql
--
-- Slice 4B-2: Opening Balance Migration & Ledger Cutover
--
-- Responsibilities:
-- 1. Fail-fast validation of legacy pre-conditions:
--    - Reject negative balances (accounts.balance < 0).
--    - Reject non-LKR currencies.
--    - Reject pre-existing cutover markers or partial ledger data.
-- 2. Capture a single deterministic UTC cutover timestamp.
-- 3. Persist ledger_cutover_at marker in system_configs.
-- 4. Create system asset accounts:
--    - SYSTEM_VAULT_CASH:LKR
--    - SYSTEM_OPENING_BALANCE:LKR
-- 5. Create customer LIABILITY ledger accounts (1:1 for every account).
-- 6. For accounts with balance > 0, create OPENING_BALANCE journal entry.
-- 7. Create balanced postings:
--    - Posting 0: DEBIT SYSTEM_OPENING_BALANCE:LKR
--    - Posting 1: CREDIT customer liability account
-- 8. Zero changes to legacy transactions table.
-- 9. Zero changes to accounts.balance.
-- ==============================================================================

DROP PROCEDURE IF EXISTS `sp_migrate_opening_balances`;

DELIMITER $$
CREATE PROCEDURE `sp_migrate_opening_balances`()
BEGIN
  DECLARE v_neg_count INT DEFAULT 0;
  DECLARE v_non_lkr_count INT DEFAULT 0;
  DECLARE v_cutover_exists INT DEFAULT 0;
  DECLARE v_ledger_acc_count INT DEFAULT 0;
  DECLARE v_journal_entry_count INT DEFAULT 0;
  DECLARE v_journal_posting_count INT DEFAULT 0;

  DECLARE v_cutover_time DATETIME(6);
  DECLARE v_opening_asset_id BINARY(16);
  DECLARE v_vault_cash_id BINARY(16);
  DECLARE v_config_id BINARY(16);

  -- --------------------------------------------------------------------------
  -- 1. Precondition checks (Fail fast before any DML)
  -- --------------------------------------------------------------------------

  -- Check A: Reject negative balances
  SELECT COUNT(*) INTO v_neg_count FROM `accounts` WHERE `balance` < 0;
  IF v_neg_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Precondition failed: Negative account balance detected in legacy accounts';
  END IF;

  -- Check B: Reject non-LKR currencies
  SELECT COUNT(*) INTO v_non_lkr_count FROM `accounts` WHERE `currency` != 'LKR';
  IF v_non_lkr_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Precondition failed: Non-LKR account currency detected';
  END IF;

  -- Check C: Reject pre-existing cutover marker
  SELECT COUNT(*) INTO v_cutover_exists FROM `system_configs` WHERE `config_key` = 'ledger_cutover_at';
  IF v_cutover_exists > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Precondition failed: ledger_cutover_at config already exists';
  END IF;

  -- Check D: Reject partial pre-existing ledger data
  SELECT COUNT(*) INTO v_ledger_acc_count FROM `ledger_accounts`;
  IF v_ledger_acc_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Precondition failed: ledger_accounts table is not empty prior to cutover';
  END IF;

  SELECT COUNT(*) INTO v_journal_entry_count FROM `journal_entries`;
  IF v_journal_entry_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Precondition failed: journal_entries table is not empty prior to cutover';
  END IF;

  SELECT COUNT(*) INTO v_journal_posting_count FROM `journal_postings`;
  IF v_journal_posting_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Precondition failed: journal_postings table is not empty prior to cutover';
  END IF;

  -- --------------------------------------------------------------------------
  -- 2. Capture Single Cutover Timestamp & Identifiers
  -- --------------------------------------------------------------------------
  SET v_cutover_time = UTC_TIMESTAMP(6);
  SET v_opening_asset_id = UUID_TO_BIN(UUID(), 0);
  SET v_vault_cash_id = UUID_TO_BIN(UUID(), 0);
  SET v_config_id = UUID_TO_BIN(UUID(), 0);

  -- --------------------------------------------------------------------------
  -- 3. Persist Cutover Timestamp in system_configs
  -- --------------------------------------------------------------------------
  INSERT INTO `system_configs` (`config_id`, `config_key`, `config_type`, `config_value`, `description`)
  VALUES (
    v_config_id,
    'ledger_cutover_at',
    'DATETIME',
    DATE_FORMAT(v_cutover_time, '%Y-%m-%dT%H:%i:%s.%fZ'),
    'Timestamp recording when double-entry ledger authority began'
  );

  -- --------------------------------------------------------------------------
  -- 4. Create System Asset Accounts
  -- --------------------------------------------------------------------------
  INSERT INTO `ledger_accounts` (
    `ledger_account_id`, `customer_account_id`, `system_code`,
    `account_class`, `currency`, `status`, `created_at`
  )
  VALUES 
    (v_vault_cash_id, NULL, 'SYSTEM_VAULT_CASH:LKR', 'ASSET', 'LKR', 'ACTIVE', v_cutover_time),
    (v_opening_asset_id, NULL, 'SYSTEM_OPENING_BALANCE:LKR', 'ASSET', 'LKR', 'ACTIVE', v_cutover_time);

  -- --------------------------------------------------------------------------
  -- 5. Create Customer LIABILITY Ledger Accounts (1:1 mapping)
  -- --------------------------------------------------------------------------
  INSERT INTO `ledger_accounts` (
    `ledger_account_id`, `customer_account_id`, `system_code`,
    `account_class`, `currency`, `status`, `created_at`
  )
  SELECT 
    UUID_TO_BIN(UUID(), 0),
    a.`account_id`,
    NULL,
    'LIABILITY',
    a.`currency`,
    CASE 
      WHEN a.`account_status` = 'ACTIVE' THEN 'ACTIVE'
      WHEN a.`account_status` = 'FROZEN' THEN 'FROZEN'
      WHEN a.`account_status` = 'CLOSED' THEN 'CLOSED'
      ELSE 'ACTIVE'
    END,
    v_cutover_time
  FROM `accounts` a;

  -- --------------------------------------------------------------------------
  -- 6. Create OPENING_BALANCE Journal Entries (Only for balance > 0)
  -- --------------------------------------------------------------------------
  INSERT INTO `journal_entries` (
    `entry_id`,
    `entry_reference`,
    `entry_type`,
    `status`,
    `currency`,
    `total_amount`,
    `description`,
    `reversal_of_entry_id`,
    `actor_type`,
    `initiated_by_user_id`,
    `system_actor_id`,
    `channel`,
    `posted_at`
  )
  SELECT 
    UUID_TO_BIN(UUID(), 0),
    CONCAT('CUTOVER-', LOWER(HEX(a.`account_id`))),
    'OPENING_BALANCE',
    'POSTED',
    a.`currency`,
    a.`balance`,
    CONCAT('Opening balance cutover for account ', a.`account_number`),
    NULL,
    'SYSTEM',
    NULL,
    'MIGRATION_CUTOVER',
    'SYSTEM',
    v_cutover_time
  FROM `accounts` a
  WHERE a.`balance` > 0;

  -- --------------------------------------------------------------------------
  -- 7. Create Posting 0: DEBIT SYSTEM_OPENING_BALANCE:LKR
  -- --------------------------------------------------------------------------
  INSERT INTO `journal_postings` (
    `posting_id`,
    `entry_id`,
    `ledger_account_id`,
    `sequence_number`,
    `direction`,
    `amount`,
    `currency`,
    `created_at`
  )
  SELECT 
    UUID_TO_BIN(UUID(), 0),
    je.`entry_id`,
    v_opening_asset_id,
    0,
    'DEBIT',
    je.`total_amount`,
    je.`currency`,
    v_cutover_time
  FROM `journal_entries` je
  WHERE je.`entry_type` = 'OPENING_BALANCE' AND je.`system_actor_id` = 'MIGRATION_CUTOVER';

  -- --------------------------------------------------------------------------
  -- 8. Create Posting 1: CREDIT Customer Liability Account
  -- --------------------------------------------------------------------------
  INSERT INTO `journal_postings` (
    `posting_id`,
    `entry_id`,
    `ledger_account_id`,
    `sequence_number`,
    `direction`,
    `amount`,
    `currency`,
    `created_at`
  )
  SELECT 
    UUID_TO_BIN(UUID(), 0),
    je.`entry_id`,
    la.`ledger_account_id`,
    1,
    'CREDIT',
    je.`total_amount`,
    je.`currency`,
    v_cutover_time
  FROM `journal_entries` je
  JOIN `accounts` a ON je.`entry_reference` = CONCAT('CUTOVER-', LOWER(HEX(a.`account_id`)))
  JOIN `ledger_accounts` la ON la.`customer_account_id` = a.`account_id`
  WHERE je.`entry_type` = 'OPENING_BALANCE' AND je.`system_actor_id` = 'MIGRATION_CUTOVER';

END$$
DELIMITER ;

CALL `sp_migrate_opening_balances`();
DROP PROCEDURE `sp_migrate_opening_balances`;
