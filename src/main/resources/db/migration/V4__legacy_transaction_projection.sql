-- V4__legacy_transaction_projection.sql
-- Synchronous legacy transaction projection link and precision adjustment

ALTER TABLE transactions
    MODIFY COLUMN amount DECIMAL(19,4) NOT NULL,
    MODIFY COLUMN balance_after DECIMAL(19,4) NOT NULL,
    ADD COLUMN journal_entry_id BINARY(16) NULL,
    ADD CONSTRAINT fk_transactions_journal_entry
        FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (entry_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT uq_transactions_journal_account
        UNIQUE (journal_entry_id, account_id);
