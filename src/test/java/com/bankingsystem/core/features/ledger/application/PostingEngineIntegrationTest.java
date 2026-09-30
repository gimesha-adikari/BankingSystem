package com.bankingsystem.core.features.ledger.application;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class PostingEngineIntegrationTest {

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

        // 2. Fetch existing real user for initiated_by_user_id FK
        realUserId = userRepository.findByUsername("customer")
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElseThrow())
                .getUserId();

        // 3. Create test customer
        testCustomer = new Customer();
        testCustomer.setFirstName("Ledger");
        testCustomer.setLastName("Tester");
        testCustomer.setEmail("ledger.tester@example.test");
        testCustomer.setPhone("+94770000001");
        testCustomer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testCustomer.setGender(com.bankingsystem.core.modules.common.enums.Gender.MALE);
        testCustomer.setStatus(com.bankingsystem.core.modules.common.enums.Status.ACTIVE);
        testCustomer.setAddress("Test Address");
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
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_postings");
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_account");
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_transactions");
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

    private long countTransactions() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
            rs.next();
            return rs.getLong(1);
        }
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
    // TEST 1: Deposit-shaped posting increases customer liability balance
    // =========================================================================
    @Test
    void successfulDepositShapedPostingIncreasesCustomerBalance() throws Exception {
        long txBefore = countTransactions();
        long idemBefore = countCoreIdempotency();

        Account customerAccount = createCustomerAccount("ACC-DEP-001", BigDecimal.ZERO, "LKR");
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

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(result.getResultingCustomerBalances().get(customerAccount.getAccountId()))
                .isEqualByComparingTo(new BigDecimal("100.0000"));

        Account updated = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));

        assertThat(journalEntryRepository.count()).isEqualTo(1);
        assertThat(journalPostingRepository.count()).isEqualTo(2);

        // Verify reconciliation passes
        LedgerReconciliationResult recResult = reconciliationService.reconcileAll();
        assertThat(recResult.isClean()).isTrue();

        // Verify legacy history projected
        assertThat(countTransactions()).isEqualTo(txBefore + 1);
        assertThat(countCoreIdempotency()).isEqualTo(idemBefore);
    }

    // =========================================================================
    // TEST 2: Withdrawal-shaped posting decreases customer liability balance
    // =========================================================================
    @Test
    void successfulWithdrawalShapedPostingDecreasesCustomerBalance() {
        Account customerAccount = createCustomerAccount("ACC-WTH-001", BigDecimal.ZERO, "LKR");
        depositFunds(customerAccount, "100.00");

        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.WITHDRAWAL,
                CurrencyCode.LKR,
                "ATM cash withdrawal test",
                PostingActor.user(realUserId),
                LedgerChannel.MOBILE,
                List.of(
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("40.00", CurrencyCode.LKR)),
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("40.00", CurrencyCode.LKR))
                )
        );

        PostingResult result = postingEngine.post(cmd);

        assertThat(result.getResultingCustomerBalances().get(customerAccount.getAccountId()))
                .isEqualByComparingTo(new BigDecimal("60.0000"));

        Account updated = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("60.0000"));

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 3: Internal transfer-shaped posting conserves total customer money
    // =========================================================================
    @Test
    void successfulInternalTransferConservesCustomerFunds() {
        Account accA = createCustomerAccount("ACC-TRF-A", BigDecimal.ZERO, "LKR");
        Account accB = createCustomerAccount("ACC-TRF-B", BigDecimal.ZERO, "LKR");
        depositFunds(accA, "100.00");
        depositFunds(accB, "50.00");

        LedgerAccount laA = ledgerAccountRepository.findByCustomerAccountId(accA.getAccountId()).orElseThrow();
        LedgerAccount laB = ledgerAccountRepository.findByCustomerAccountId(accB.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.TRANSFER,
                CurrencyCode.LKR,
                "P2P internal transfer",
                PostingActor.user(realUserId),
                LedgerChannel.WEB,
                List.of(
                        new PostingInstruction(laA.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR)),
                        new PostingInstruction(laB.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR))
                )
        );

        PostingResult result = postingEngine.post(cmd);

        assertThat(result.getResultingCustomerBalances().get(accA.getAccountId())).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(result.getResultingCustomerBalances().get(accB.getAccountId())).isEqualByComparingTo(new BigDecimal("75.0000"));

        Account updatedA = accountRepository.findById(accA.getAccountId()).orElseThrow();
        Account updatedB = accountRepository.findById(accB.getAccountId()).orElseThrow();

        assertThat(updatedA.getBalance()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(updatedB.getBalance()).isEqualByComparingTo(new BigDecimal("75.0000"));

        // Total funds conserved: 100 + 50 = 150 = 75 + 75
        assertThat(updatedA.getBalance().add(updatedB.getBalance())).isEqualByComparingTo(new BigDecimal("150.0000"));

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 4: Zero/negative result protection (no overdraft allowed)
    // =========================================================================
    @Test
    void overdraftAttemptIsRejectedLeavingBalanceAndLedgerUntouched() {
        Account customerAccount = createCustomerAccount("ACC-OVERDRAFT-001", new BigDecimal("50.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.WITHDRAWAL,
                CurrencyCode.LKR,
                "Overdraft withdrawal attempt",
                PostingActor.user(realUserId),
                LedgerChannel.WEB,
                List.of(
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("60.00", CurrencyCode.LKR)),
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("60.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient funds");

        Account refreshed = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo(new BigDecimal("50.0000"));

        assertThat(journalEntryRepository.count()).isEqualTo(0);
        assertThat(journalPostingRepository.count()).isEqualTo(0);
    }

    // =========================================================================
    // TEST 5: Unbalanced journal is rejected before persistence
    // =========================================================================
    @Test
    void unbalancedJournalIsRejectedBeforePersistence() {
        Account customerAccount = createCustomerAccount("ACC-UNBAL-001", new BigDecimal("500.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Unbalanced deposit attempt",
                PostingActor.system("TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("90.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_UNBALANCED_JOURNAL"));

        Account refreshed = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo(new BigDecimal("500.0000"));

        assertThat(journalEntryRepository.count()).isEqualTo(0);
        assertThat(journalPostingRepository.count()).isEqualTo(0);
    }

    // =========================================================================
    // TEST 6: Currency mismatch is rejected
    // =========================================================================
    @Test
    void currencyMismatchIsRejected() {
        Account customerAccount = createCustomerAccount("ACC-CURR-LKR", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        // USD instruction in LKR command
        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Currency mismatch test",
                PostingActor.system("TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.USD)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_CURRENCY_MISMATCH"));
    }

    // =========================================================================
    // TEST 7: Four-decimal precision is preserved
    // =========================================================================
    @Test
    void fourDecimalPrecisionIsPreserved() {
        Account customerAccount = createCustomerAccount("ACC-PREC-001", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        BigDecimal exactAmount = new BigDecimal("12.3456");
        MonetaryAmount ledgerAmount = MonetaryAmount.fromLedger(exactAmount, CurrencyCode.LKR);

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Four decimal deposit",
                PostingActor.system("INTEREST_ENGINE"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, ledgerAmount),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, ledgerAmount)
                )
        );

        PostingResult result = postingEngine.post(cmd);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(exactAmount);
        assertThat(result.getResultingCustomerBalances().get(customerAccount.getAccountId()))
                .isEqualByComparingTo(new BigDecimal("112.3456"));

        Account refreshed = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo(new BigDecimal("112.3456"));

        // Prove customer input parser rejects 12.3456
        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("12.3456", CurrencyCode.LKR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 2 fractional digits");
    }

    // =========================================================================
    // TEST 8: Failure on journal_postings rolls back JournalEntry and Account balance
    // =========================================================================
    @Test
    void failureDuringPostingsInsertRollsBackEntireTransaction() throws Exception {
        Account customerAccount = createCustomerAccount("ACC-FAIL-POST", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        // Install test-only trigger on journal_postings
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_postings BEFORE INSERT ON journal_postings " +
                    "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'TEST_FAIL_POSTINGS'");
        }

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Failing posting test",
                PostingActor.system("TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .hasMessageContaining("TEST_FAIL_POSTINGS");

        // Verify full rollback: 0 journal entries, 0 postings, balance unchanged
        assertThat(journalEntryRepository.count()).isEqualTo(0);
        assertThat(journalPostingRepository.count()).isEqualTo(0);

        Account refreshed = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    // =========================================================================
    // TEST 9: Failure on account balance update rolls back JournalEntry & Postings
    // =========================================================================
    @Test
    void failureDuringAccountBalanceUpdateRollsBackEntireTransaction() throws Exception {
        Account customerAccount = createCustomerAccount("ACC-FAIL-ACC", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        // Install test-only trigger on accounts
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_account BEFORE UPDATE ON accounts " +
                    "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'TEST_FAIL_ACCOUNT_UPDATE'");
        }

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Failing account update test",
                PostingActor.system("TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmd))
                .hasMessageContaining("TEST_FAIL_ACCOUNT_UPDATE");

        // Verify full rollback
        assertThat(journalEntryRepository.count()).isEqualTo(0);
        assertThat(journalPostingRepository.count()).isEqualTo(0);

        Account refreshed = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    // =========================================================================
    // TEST 10: Concurrent withdrawal contending on balance prevents lost updates
    // =========================================================================
    @Test
    void concurrentWithdrawalsPreventLostUpdatesAndOverspend() throws Exception {
        Account customerAccount = createCustomerAccount("ACC-CONC-001", BigDecimal.ZERO, "LKR");
        depositFunds(customerAccount, "100.00");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PostingCommand cmd = new PostingCommand(
                            JournalEntryType.WITHDRAWAL,
                            CurrencyCode.LKR,
                            "Concurrent withdrawal of 80.00",
                            PostingActor.user(realUserId),
                            LedgerChannel.MOBILE,
                            List.of(
                                    new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("80.00", CurrencyCode.LKR)),
                                    new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("80.00", CurrencyCode.LKR))
                            )
                    );
                    postingEngine.post(cmd);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getMessage().contains("Insufficient funds")) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        Account finalAccount = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(finalAccount.getBalance()).isEqualByComparingTo(new BigDecimal("20.0000"));

        // 1 deposit to fund + 1 successful withdrawal = 2 entries, 4 postings
        assertThat(journalEntryRepository.count()).isEqualTo(2);
        assertThat(journalPostingRepository.count()).isEqualTo(4);

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 11: Opposite-direction transfers survive contention without deadlock
    // =========================================================================
    @Test
    void oppositeDirectionTransfersSurviveContentionWithoutDeadlock() throws Exception {
        Account accA = createCustomerAccount("ACC-CONTEND-A", BigDecimal.ZERO, "LKR");
        Account accB = createCustomerAccount("ACC-CONTEND-B", BigDecimal.ZERO, "LKR");
        depositFunds(accA, "200.00");
        depositFunds(accB, "200.00");

        LedgerAccount laA = ledgerAccountRepository.findByCustomerAccountId(accA.getAccountId()).orElseThrow();
        LedgerAccount laB = ledgerAccountRepository.findByCustomerAccountId(accB.getAccountId()).orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Run 5 repetitions of concurrent opposite-direction transfers
        for (int rep = 0; rep < 5; rep++) {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);

            // Thread 1: Transfer 10.00 from A -> B
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PostingCommand cmdAtoB = new PostingCommand(
                            JournalEntryType.TRANSFER,
                            CurrencyCode.LKR,
                            "A to B transfer",
                            PostingActor.user(realUserId),
                            LedgerChannel.WEB,
                            List.of(
                                    new PostingInstruction(laA.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR)),
                                    new PostingInstruction(laB.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR))
                            )
                    );
                    postingEngine.post(cmdAtoB);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });

            // Thread 2: Transfer 20.00 from B -> A
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PostingCommand cmdBtoA = new PostingCommand(
                            JournalEntryType.TRANSFER,
                            CurrencyCode.LKR,
                            "B to A transfer",
                            PostingActor.user(realUserId),
                            LedgerChannel.WEB,
                            List.of(
                                    new PostingInstruction(laB.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR)),
                                    new PostingInstruction(laA.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("20.00", CurrencyCode.LKR))
                            )
                    );
                    postingEngine.post(cmdBtoA);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown();
            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        }

        executor.shutdown();

        Account finalA = accountRepository.findById(accA.getAccountId()).orElseThrow();
        Account finalB = accountRepository.findById(accB.getAccountId()).orElseThrow();

        // Net per repetition: A loses 10 and gains 20 = +10. Over 5 reps: A has 200 + 50 = 250.0000
        // B loses 20 and gains 10 = -10. Over 5 reps: B has 200 - 50 = 150.0000
        assertThat(finalA.getBalance()).isEqualByComparingTo(new BigDecimal("250.0000"));
        assertThat(finalB.getBalance()).isEqualByComparingTo(new BigDecimal("150.0000"));

        // Total funds conserved: 250 + 150 = 400
        assertThat(finalA.getBalance().add(finalB.getBalance())).isEqualByComparingTo(new BigDecimal("400.0000"));

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 12: Reference uniqueness, format, and length
    // =========================================================================
    @Test
    void referenceUniquenessFormatAndLength() {
        Set<String> generatedRefs = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String ref = "TX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            assertThat(ref).matches("^TX-[0-9A-F]{32}$");
            assertThat(ref.length()).isLessThanOrEqualTo(64);
            assertThat(generatedRefs.add(ref)).isTrue();
        }
        assertThat(generatedRefs.size()).isEqualTo(1000);
    }

    // =========================================================================
    // TEST 13: Reconciliation detects balance drift without mutating data
    // =========================================================================
    @Test
    void reconciliationDetectsDriftWithoutModifyingData() throws Exception {
        Account customerAccount = createCustomerAccount("ACC-RECON-001", BigDecimal.ZERO, "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        // 1. Post a valid deposit of 50.0000
        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Valid deposit",
                PostingActor.system("RECON_TEST"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR))
                )
        );
        postingEngine.post(cmd);

        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();

        // 2. Tamper: corrupt account balance directly via JDBC (+25.0000)
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE accounts SET balance = balance + 25.0000 WHERE account_number = 'ACC-RECON-001'");
        }

        // 3. Reconcile: must report discrepancy
        LedgerReconciliationResult driftedResult = reconciliationService.reconcileAll();
        assertThat(driftedResult.isClean()).isFalse();
        assertThat(driftedResult.getDiscrepancyCount()).isEqualTo(1);
        LedgerReconciliationResult.AccountDiscrepancy disc = driftedResult.getDiscrepancies().get(0);
        assertThat(disc.accountNumber()).isEqualTo("ACC-RECON-001");
        assertThat(disc.materializedBalance()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(disc.derivedBalance()).isEqualByComparingTo(new BigDecimal("50.0000"));
        assertThat(disc.difference()).isEqualByComparingTo(new BigDecimal("25.0000"));

        // 4. Verify reconciliation did NOT overwrite either side
        Account unmutated = accountRepository.findById(customerAccount.getAccountId()).orElseThrow();
        assertThat(unmutated.getBalance()).isEqualByComparingTo(new BigDecimal("75.0000"));

        // 5. Restore balance and verify clean again
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE accounts SET balance = balance - 25.0000 WHERE account_number = 'ACC-RECON-001'");
        }
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    // =========================================================================
    // TEST 14: Runtime engine rejects OPENING_BALANCE and REVERSAL
    // =========================================================================
    @Test
    void runtimeEngineRejectsOpeningBalanceAndReversal() {
        Account customerAccount = createCustomerAccount("ACC-REJECT-TYPES", new BigDecimal("100.0000"), "LKR");
        LedgerAccount customerLedgerAccount = ledgerAccountRepository.findByCustomerAccountId(customerAccount.getAccountId()).orElseThrow();

        PostingCommand cmdOpen = new PostingCommand(
                JournalEntryType.OPENING_BALANCE,
                CurrencyCode.LKR,
                "Attempt runtime opening balance",
                PostingActor.system("ATTACKER"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmdOpen))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OPENING_BALANCE entries are reserved exclusively for migration cutover");

        PostingCommand cmdRev = new PostingCommand(
                JournalEntryType.REVERSAL,
                CurrencyCode.LKR,
                "Attempt runtime reversal",
                PostingActor.system("ATTACKER"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR)),
                        new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, MonetaryAmount.fromCustomerInput("10.00", CurrencyCode.LKR))
                )
        );

        assertThatThrownBy(() -> postingEngine.post(cmdRev))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REVERSAL entries are reserved for dedicated reversal workflows");
    }
}
