package com.bankingsystem.core.features.transactions;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationResult;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationService;
import com.bankingsystem.core.features.ledger.application.PostingCommand;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.application.PostingInstruction;
import com.bankingsystem.core.features.ledger.application.PostingResult;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.TransactionQueryService;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.features.transactions.interfaces.dto.TransactionResponseDTO;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class LegacyTransactionProjectionIntegrationTest {

    @Autowired
    private PostingEngine postingEngine;

    @Autowired
    private LedgerReconciliationService reconciliationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalPostingRepository journalPostingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionQueryService transactionQueryService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    private Customer testCustomer;
    private LedgerAccount vaultCashLedgerAccount;
    private UUID realUserId;

    @BeforeEach
    void setUp() throws Exception {
        cleanTestData();

        // 1. Ensure system vault account exists
        vaultCashLedgerAccount = ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR")
                .orElseGet(() -> {
                    LedgerAccount sys = new LedgerAccount(
                            UUID.randomUUID(),
                            null,
                            "SYSTEM_VAULT_CASH:LKR",
                            LedgerAccountClass.ASSET,
                            "LKR",
                            LedgerAccountStatus.ACTIVE,
                            LocalDateTime.now(ZoneOffset.UTC)
                    );
                    return ledgerAccountRepository.save(sys);
                });

        // 2. Fetch existing real user
        realUserId = userRepository.findByUsername("customer")
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElseThrow())
                .getUserId();

        // 3. Create test customer
        testCustomer = new Customer();
        testCustomer.setFirstName("Projection");
        testCustomer.setLastName("Tester");
        testCustomer.setEmail("proj.tester@example.test");
        testCustomer.setPhone("+94770000088");
        testCustomer.setDateOfBirth(LocalDate.of(1992, 2, 2));
        testCustomer.setGender(com.bankingsystem.core.modules.common.enums.Gender.MALE);
        testCustomer.setStatus(com.bankingsystem.core.modules.common.enums.Status.ACTIVE);
        testCustomer.setAddress("Test Projection Address");
        testCustomer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        testCustomer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        testCustomer = customerRepository.save(testCustomer);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_transactions");
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_transfer_in");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM journal_postings");
            stmt.execute("DELETE FROM journal_entries");
            stmt.execute("DELETE FROM ledger_accounts WHERE customer_account_id IS NOT NULL");
            stmt.execute("DELETE FROM accounts");
            stmt.execute("DELETE FROM customers WHERE email LIKE '%@example.test'");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private Account createCustomerAccount(String accNumber, BigDecimal initialBalance, String currency) {
        Account account = new Account();
        account.setAccountNumber(accNumber);
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(initialBalance.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY));
        account.setCurrency(currency);
        account.setCustomer(testCustomer);
        account.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account = accountRepository.save(account);

        LedgerAccount ledgerAccount = new LedgerAccount(
                UUID.randomUUID(),
                account.getAccountId(),
                null,
                LedgerAccountClass.LIABILITY,
                currency,
                LedgerAccountStatus.ACTIVE,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        ledgerAccountRepository.save(ledgerAccount);

        return account;
    }

    private void depositFunds(Account account, String amountStr) {
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(account.getAccountId()).orElseThrow();
        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Test funding deposit",
                PostingActor.system("TEST_SEEDER"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput(amountStr, CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput(amountStr, CurrencyCode.LKR))
                )
        );
        postingEngine.post(cmd);
    }

    private long countCoreIdempotency() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM core_transaction_idempotency")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // =========================================================================
    // TEST 1: Deposit-shaped posting creates exactly one legacy DEPOSIT transaction
    // =========================================================================
    @Test
    void depositShapedPostingCreatesOneLegacyDepositTransaction() throws Exception {
        long idemBefore = countCoreIdempotency();

        Account customerAccount = createCustomerAccount("ACC-PROJ-DEP", BigDecimal.ZERO, "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Cash deposit test",
                PostingActor.system("TELLER_TERMINAL_1"),
                LedgerChannel.TELLER,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR))
                )
        );

        PostingResult result = postingEngine.post(cmd);

        // 1. Ledger assertions
        assertThat(journalEntryRepository.count()).isEqualTo(1);
        assertThat(journalPostingRepository.count()).isEqualTo(2);

        // 2. Account balance assertion
        Account updated = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));

        // 3. Legacy transaction projection assertions
        List<Transaction> txs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(customerAccount.getAccountId());
        assertThat(txs).hasSize(1);
        Transaction tx = txs.get(0);
        assertThat(tx.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(tx.getDescription()).isEqualTo("Cash deposit test");
        assertThat(tx.getJournalEntryId()).isEqualTo(result.getEntryId());
        assertThat(tx.getCreatedAt()).isEqualTo(result.getPostedAt());

        // 4. Invariant: zero transactions created for system vault cash
        assertThat(transactionRepository.count()).isEqualTo(1);

        // 5. Invariant: idempotency table untouched
        assertThat(countCoreIdempotency()).isEqualTo(idemBefore);

        // 6. Reconciliation clean
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 2: Withdrawal-shaped posting creates exactly one legacy WITHDRAWAL transaction
    // =========================================================================
    @Test
    void withdrawalShapedPostingCreatesOneLegacyWithdrawalTransaction() {
        Account customerAccount = createCustomerAccount("ACC-PROJ-WTH", BigDecimal.ZERO, "LKR");
        depositFunds(customerAccount, "100.00");

        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.WITHDRAWAL,
                CurrencyCode.LKR,
                "ATM cash withdrawal",
                PostingActor.user(realUserId),
                LedgerChannel.MOBILE,
                List.of(
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("40.00", CurrencyCode.LKR)),
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("40.00", CurrencyCode.LKR))
                )
        );

        PostingResult result = postingEngine.post(cmd);

        // Materialized balance
        Account updated = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("60.0000"));

        // Transactions: 1 from deposit + 1 from withdrawal = 2 total
        List<Transaction> txs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(customerAccount.getAccountId());
        assertThat(txs).hasSize(2);

        Transaction withdrawalTx = txs.get(0); // newest first
        assertThat(withdrawalTx.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(withdrawalTx.getAmount()).isEqualByComparingTo(new BigDecimal("40.0000"));
        assertThat(withdrawalTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("60.0000"));
        assertThat(withdrawalTx.getDescription()).isEqualTo("ATM cash withdrawal");
        assertThat(withdrawalTx.getJournalEntryId()).isEqualTo(result.getEntryId());
        assertThat(withdrawalTx.getCreatedAt()).isEqualTo(result.getPostedAt());

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 3: Transfer-shaped posting creates exactly two transactions sharing journal ID
    // =========================================================================
    @Test
    void transferShapedPostingCreatesTwoLegacyTransactionsSharingJournalId() {
        Account accA = createCustomerAccount("ACC-PROJ-TRF-A", BigDecimal.ZERO, "LKR");
        Account accB = createCustomerAccount("ACC-PROJ-TRF-B", BigDecimal.ZERO, "LKR");
        depositFunds(accA, "100.00");
        depositFunds(accB, "50.00");

        LedgerAccount laA = ledgerAccountRepository.findByCustomerAccountId(accA.getAccountId()).orElseThrow();
        LedgerAccount laB = ledgerAccountRepository.findByCustomerAccountId(accB.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.TRANSFER,
                CurrencyCode.LKR,
                "P2P payment from A to B",
                PostingActor.user(realUserId),
                LedgerChannel.WEB,
                List.of(
                        new PostingInstruction(laA.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR)),
                        new PostingInstruction(laB.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR))
                )
        );

        PostingResult result = postingEngine.post(cmd);

        Account finalA = accountRepository.findById(accA.getAccountId()).orElseThrow();
        Account finalB = accountRepository.findById(accB.getAccountId()).orElseThrow();
        assertThat(finalA.getBalance()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(finalB.getBalance()).isEqualByComparingTo(new BigDecimal("75.0000"));

        // Source A projection: TRANSFER_OUT
        List<Transaction> txsA = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(accA.getAccountId());
        Transaction outTx = txsA.get(0);
        assertThat(outTx.getType()).isEqualTo(Transaction.TransactionType.TRANSFER_OUT);
        assertThat(outTx.getAmount()).isEqualByComparingTo(new BigDecimal("25.0000"));
        assertThat(outTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(outTx.getJournalEntryId()).isEqualTo(result.getEntryId());
        assertThat(outTx.getCreatedAt()).isEqualTo(result.getPostedAt());

        // Destination B projection: TRANSFER_IN
        List<Transaction> txsB = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(accB.getAccountId());
        Transaction inTx = txsB.get(0);
        assertThat(inTx.getType()).isEqualTo(Transaction.TransactionType.TRANSFER_IN);
        assertThat(inTx.getAmount()).isEqualByComparingTo(new BigDecimal("25.0000"));
        assertThat(inTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(inTx.getJournalEntryId()).isEqualTo(result.getEntryId());
        assertThat(inTx.getCreatedAt()).isEqualTo(result.getPostedAt());

        // Critical invariants:
        // 1. Both share the exact same journal_entry_id
        assertThat(outTx.getJournalEntryId()).isEqualTo(inTx.getJournalEntryId());
        // 2. Different transaction UUIDs
        assertThat(outTx.getTransactionId()).isNotEqualTo(inTx.getTransactionId());
        // 3. Total funds conserved: 75 + 75 = 150
        assertThat(finalA.getBalance().add(finalB.getBalance())).isEqualByComparingTo(new BigDecimal("150.0000"));

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 4: Four-decimal precision is preserved in legacy projection
    // =========================================================================
    @Test
    void fourDecimalPrecisionPreservedInLegacyProjection() {
        Account customerAccount = createCustomerAccount("ACC-PROJ-PREC", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        BigDecimal exactAmount = new BigDecimal("12.3456");
        MonetaryAmount ledgerAmount = MonetaryAmount.fromLedger(exactAmount, CurrencyCode.LKR);

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Exact interest credit",
                PostingActor.system("INTEREST_SERVICE"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, ledgerAmount),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, ledgerAmount)
                )
        );

        PostingResult result = postingEngine.post(cmd);

        List<Transaction> txs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(customerAccount.getAccountId());
        assertThat(txs).hasSize(1);
        Transaction tx = txs.get(0);
        // Must preserve all 4 decimals, never rounded to 12.35
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("12.3456"));
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("112.3456"));
    }

    // =========================================================================
    // TEST 5: Legacy history query returns projections and historical rows newest first
    // =========================================================================
    @Test
    void legacyHistoryQueryReturnsProjectionsAndHistoricalRowsNewestFirst() {
        Account customerAccount = createCustomerAccount("ACC-PROJ-HIST", BigDecimal.ZERO, "LKR");

        // 1. Insert an older historical row with journal_entry_id = null
        Transaction historical = new Transaction();
        historical.setAccount(customerAccount);
        historical.setType(Transaction.TransactionType.DEPOSIT);
        historical.setAmount(new BigDecimal("500.0000"));
        historical.setBalanceAfter(new BigDecimal("500.0000"));
        historical.setDescription("Historical pre-ledger deposit");
        historical.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(5));
        historical.setJournalEntryId(null);
        transactionRepository.save(historical);

        // 2. Post a new deposit via PostingEngine
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();
        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "New modern deposit",
                PostingActor.system("TELLER"),
                LedgerChannel.TELLER,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("200.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("200.00", CurrencyCode.LKR))
                )
        );
        PostingResult result = postingEngine.post(cmd);

        // 3. Query via existing TransactionQueryService
        List<TransactionResponseDTO> history = transactionQueryService.getTransactionsForAccount(customerAccount.getAccountId());

        assertThat(history).hasSize(2);
        // Order: newest first
        TransactionResponseDTO newest = history.get(0);
        assertThat(newest.getDescription()).isEqualTo("New modern deposit");
        assertThat(newest.getAmount()).isEqualByComparingTo(new BigDecimal("200.0000"));
        assertThat(newest.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("200.0000"));

        TransactionResponseDTO oldest = history.get(1);
        assertThat(oldest.getDescription()).isEqualTo("Historical pre-ledger deposit");
        assertThat(oldest.getAmount()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(oldest.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("500.0000"));
    }

    // =========================================================================
    // TEST 6: Projection failure rolls back entire posting atomically
    // =========================================================================
    @Test
    void projectionFailureRollsBackEntirePostingAtomically() throws Exception {
        Account customerAccount = createCustomerAccount("ACC-FAIL-PROJ", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        // Install test-only trigger on transactions
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_transactions BEFORE INSERT ON transactions " +
                    "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'TEST_FAIL_TRANSACTIONS'");
        }

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Failing projection test",
                PostingActor.system("TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .hasMessageContaining("TEST_FAIL_TRANSACTIONS");

        // Verify full rollback: 0 journal entries, 0 postings, 0 transactions, balance unchanged
        assertThat(journalEntryRepository.count()).isEqualTo(0);
        assertThat(journalPostingRepository.count()).isEqualTo(0);
        assertThat(transactionRepository.count()).isEqualTo(0);

        Account refreshed = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    // =========================================================================
    // TEST 7: Second transfer projection failure rolls back source projection and ledger
    // =========================================================================
    @Test
    void secondTransferProjectionFailureRollsBackSourceProjectionAndLedger() throws Exception {
        Account accA = createCustomerAccount("ACC-FAIL-TRF-A", new BigDecimal("100.0000"), "LKR");
        Account accB = createCustomerAccount("ACC-FAIL-TRF-B", new BigDecimal("50.0000"), "LKR");

        LedgerAccount laA = ledgerAccountRepository.findByCustomerAccountId(accA.getAccountId()).orElseThrow();
        LedgerAccount laB = ledgerAccountRepository.findByCustomerAccountId(accB.getAccountId()).orElseThrow();

        // Install trigger that fails ONLY on TRANSFER_IN (the second leg of the transfer projection)
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_transfer_in BEFORE INSERT ON transactions " +
                    "FOR EACH ROW BEGIN " +
                    "  IF NEW.type = 'TRANSFER_IN' THEN " +
                    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'TEST_FAIL_TRANSFER_IN'; " +
                    "  END IF; " +
                    "END");
        }

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.TRANSFER,
                CurrencyCode.LKR,
                "Partial transfer failure test",
                PostingActor.user(realUserId),
                LedgerChannel.WEB,
                List.of(
                        new PostingInstruction(laA.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR)),
                        new PostingInstruction(laB.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .hasMessageContaining("TEST_FAIL_TRANSFER_IN");

        // Verify: source TRANSFER_OUT must NOT be left orphaned! 0 transactions total
        assertThat(transactionRepository.count()).isEqualTo(0);
        assertThat(journalEntryRepository.count()).isEqualTo(0);
        assertThat(journalPostingRepository.count()).isEqualTo(0);

        Account finalA = accountRepository.findById(accA.getAccountId()).orElseThrow();
        Account finalB = accountRepository.findById(accB.getAccountId()).orElseThrow();
        assertThat(finalA.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(finalB.getBalance()).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    // =========================================================================
    // TEST 8: Unique projection constraint prevents duplicate projection per account
    // =========================================================================
    @Test
    void uniqueProjectionConstraintPreventsDuplicatePerAccountPerJournal() {
        Account customerAccount = createCustomerAccount("ACC-PROJ-UQ", BigDecimal.ZERO, "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Original deposit",
                PostingActor.system("TELLER"),
                LedgerChannel.TELLER,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR))
                )
        );
        PostingResult result = postingEngine.post(cmd);

        // Attempt to insert duplicate projection for same journal_entry_id and same account_id
        Transaction dup = new Transaction();
        dup.setAccount(customerAccount);
        dup.setType(Transaction.TransactionType.DEPOSIT);
        dup.setAmount(new BigDecimal("50.0000"));
        dup.setBalanceAfter(new BigDecimal("50.0000"));
        dup.setDescription("Duplicate projection attempt");
        dup.setCreatedAt(result.getPostedAt());
        dup.setJournalEntryId(result.getEntryId());

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_transactions_journal_account");

        // Prove multiple historical rows with journal_entry_id = null ARE allowed
        Transaction null1 = new Transaction();
        null1.setAccount(customerAccount);
        null1.setType(Transaction.TransactionType.DEPOSIT);
        null1.setAmount(new BigDecimal("10.0000"));
        null1.setBalanceAfter(new BigDecimal("60.0000"));
        null1.setDescription("Null journal 1");
        null1.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        null1.setJournalEntryId(null);
        transactionRepository.saveAndFlush(null1);

        Transaction null2 = new Transaction();
        null2.setAccount(customerAccount);
        null2.setType(Transaction.TransactionType.DEPOSIT);
        null2.setAmount(new BigDecimal("20.0000"));
        null2.setBalanceAfter(new BigDecimal("80.0000"));
        null2.setDescription("Null journal 2");
        null2.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        null2.setJournalEntryId(null);
        transactionRepository.saveAndFlush(null2);

        assertThat(transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(customerAccount.getAccountId())).hasSize(3);
    }

    // =========================================================================
    // TEST 9: Unsupported projection topologies fail safely
    // =========================================================================
    @Test
    void unsupportedProjectionTopologiesFailSafely() {
        // System-to-system deposit (0 customer accounts)
        PostingCommand sysToSys = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "System to system invalid deposit",
                PostingActor.system("TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR)),
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(sysToSys))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_UNSUPPORTED_PROJECTION"));
    }
}
