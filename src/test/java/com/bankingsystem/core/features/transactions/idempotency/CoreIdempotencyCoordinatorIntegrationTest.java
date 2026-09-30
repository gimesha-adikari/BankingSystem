package com.bankingsystem.core.features.transactions.idempotency;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.application.*;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.features.transactions.idempotency.application.CoreIdempotencyCoordinator;
import com.bankingsystem.core.features.transactions.idempotency.domain.*;
import com.bankingsystem.core.features.transactions.idempotency.domain.repository.CoreTransactionIdempotencyRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoreIdempotencyCoordinatorIntegrationTest {

    @Autowired
    private CoreIdempotencyCoordinator coordinator;

    @Autowired
    private CoreTransactionIdempotencyRepository idempotencyRepository;

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
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    private Customer testCustomerA;
    private Customer testCustomerB;
    private LedgerAccount vaultCashLedgerAccount;
    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void setUp() throws Exception {
        cleanTestData();

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

        var allUsers = userRepository.findAll();
        userAId = allUsers.get(0).getUserId();
        userBId = allUsers.size() > 1 ? allUsers.get(1).getUserId() : userAId;

        testCustomerA = new Customer();
        testCustomerA.setFirstName("Alice");
        testCustomerA.setLastName("Idempotency");
        testCustomerA.setEmail("alice.idem@example.test");
        testCustomerA.setPhone("+94770000010");
        testCustomerA.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        testCustomerA.setGender(com.bankingsystem.core.modules.common.enums.Gender.FEMALE);
        testCustomerA.setStatus(com.bankingsystem.core.modules.common.enums.Status.ACTIVE);
        testCustomerA.setAddress("Test Address A");
        testCustomerA.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        testCustomerA.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        testCustomerA = customerRepository.save(testCustomerA);

        testCustomerB = new Customer();
        testCustomerB.setFirstName("Bob");
        testCustomerB.setLastName("Idempotency");
        testCustomerB.setEmail("bob.idem@example.test");
        testCustomerB.setPhone("+94770000011");
        testCustomerB.setDateOfBirth(java.time.LocalDate.of(1992, 2, 2));
        testCustomerB.setGender(com.bankingsystem.core.modules.common.enums.Gender.MALE);
        testCustomerB.setStatus(com.bankingsystem.core.modules.common.enums.Status.ACTIVE);
        testCustomerB.setAddress("Test Address B");
        testCustomerB.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        testCustomerB.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        testCustomerB = customerRepository.save(testCustomerB);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_idem_complete");
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_idem_postings");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("DELETE FROM core_transaction_idempotency");
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM journal_postings");
            stmt.execute("DELETE FROM journal_entries");
            stmt.execute("DELETE FROM ledger_accounts WHERE customer_account_id IS NOT NULL");
            stmt.execute("DELETE FROM accounts");
            stmt.execute("DELETE FROM customers WHERE email LIKE '%@example.test'");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private Account createCustomerAccount(Customer customer, String accNumber, BigDecimal initialBalance) {
        Account account = new Account();
        account.setAccountNumber(accNumber);
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(initialBalance.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY));
        account.setCurrency("LKR");
        account.setCustomer(customer);
        account.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account = accountRepository.save(account);

        LedgerAccount ledgerAccount = new LedgerAccount(
                UUID.randomUUID(),
                account.getAccountId(),
                null,
                LedgerAccountClass.LIABILITY,
                "LKR",
                LedgerAccountStatus.ACTIVE,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        ledgerAccountRepository.save(ledgerAccount);
        return account;
    }

    // =========================================================================
    // 1. Same user + operation + key + payload replays without duplicate execution
    // =========================================================================
    @Test
    void testSameUserSameOperationSameKeySamePayloadReplaysWithoutDuplicateExecution() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000001", new BigDecimal("1000.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("idem-key-deposit-001");

        AtomicInteger callCount = new AtomicInteger(0);

        IdempotentAction action = () -> {
            callCount.incrementAndGet();
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Deposit test",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "{\"status\":\"success\",\"amount\":\"100.00\"}");
        };

        // First call: executes action
        IdempotentCommandResult res1 = coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, action);

        assertThat(res1.replayed()).isFalse();
        assertThat(callCount.get()).isEqualTo(1);
        assertThat(res1.httpStatusCode()).isEqualTo(201);
        assertThat(res1.responsePayload()).contains("100.00");

        // Verify database state after first call
        Account accAfter1 = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accAfter1.getBalance()).isEqualByComparingTo("1100.0000");
        assertThat(idempotencyRepository.findAll()).hasSize(1);
        CoreTransactionIdempotency savedClaim = idempotencyRepository.findAll().get(0);
        assertThat(savedClaim.getState()).isEqualTo(CoreIdempotencyState.COMPLETED);
        assertThat(savedClaim.getEntryId()).isEqualTo(res1.journalEntryId());

        long journalsAfter1 = journalEntryRepository.count();
        long postingsAfter1 = journalPostingRepository.count();
        long txsAfter1 = transactionRepository.count();

        // Second call: exact same key and payload -> must replay cached result
        IdempotentCommandResult res2 = coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, action);

        assertThat(res2.replayed()).isTrue();
        assertThat(callCount.get()).isEqualTo(1); // Action was NOT invoked a second time!
        assertThat(res2.journalEntryId()).isEqualTo(res1.journalEntryId());
        assertThat(res2.httpStatusCode()).isEqualTo(201);
        assertThat(res2.responsePayload()).isEqualTo(res1.responsePayload());

        // Verify balance, journals, postings, transactions are UNCHANGED
        Account accAfter2 = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accAfter2.getBalance()).isEqualByComparingTo("1100.0000");
        assertThat(journalEntryRepository.count()).isEqualTo(journalsAfter1);
        assertThat(journalPostingRepository.count()).isEqualTo(postingsAfter1);
        assertThat(transactionRepository.count()).isEqualTo(txsAfter1);
    }

    // =========================================================================
    // 2. Same key + different payload throws IdempotencyConflictException
    // =========================================================================
    @Test
    void testSameKeyDifferentPayloadThrowsConflictException() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000002", new BigDecimal("1000.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        MonetaryAmount amount1 = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        MonetaryAmount amount2 = MonetaryAmount.fromCustomerInput("101.00", CurrencyCode.LKR);

        String hash1 = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount1);
        String hash2 = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount2);
        IdempotencyKey key = IdempotencyKey.of("idem-conflict-key");

        AtomicInteger callCount = new AtomicInteger(0);
        IdempotentAction action = () -> {
            callCount.incrementAndGet();
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Deposit 100",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount1),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount1)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK");
        };

        coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash1, action);
        assertThat(callCount.get()).isEqualTo(1);

        // Second call with same key but hash2
        assertThatThrownBy(() -> coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash2, action))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("Incoming request hash does not match stored request hash");

        assertThat(callCount.get()).isEqualTo(1);
        Account accFinal = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accFinal.getBalance()).isEqualByComparingTo("1100.0000");
    }

    // =========================================================================
    // 3. User scope independence
    // =========================================================================
    @Test
    void testUserScopeIndependence() {
        Account accA = createCustomerAccount(testCustomerA, "ACC9000000003", new BigDecimal("500.0000"));
        Account accB = createCustomerAccount(testCustomerB, "ACC9000000004", new BigDecimal("500.0000"));
        LedgerAccount laA = ledgerAccountRepository.findByCustomerAccountId(accA.getAccountId()).orElseThrow();
        LedgerAccount laB = ledgerAccountRepository.findByCustomerAccountId(accB.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);
        String hashA = CoreRequestFingerprint.forDeposit(accA.getAccountId(), CurrencyCode.LKR, amount);
        String hashB = CoreRequestFingerprint.forDeposit(accB.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey commonKey = IdempotencyKey.of("shared-user-key");

        // User A executes with commonKey
        IdempotentCommandResult resA = coordinator.execute(userAId, CoreOperationType.DEPOSIT, commonKey, hashA, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Deposit User A",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(laA.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK-A");
        });

        // User B executes with SAME commonKey
        IdempotentCommandResult resB = coordinator.execute(userBId, CoreOperationType.DEPOSIT, commonKey, hashB, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Deposit User B",
                    PostingActor.user(userBId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(laB.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK-B");
        });

        assertThat(resA.replayed()).isFalse();
        assertThat(resB.replayed()).isFalse();
        assertThat(resA.journalEntryId()).isNotEqualTo(resB.journalEntryId());
        assertThat(idempotencyRepository.findAll()).hasSize(2);
    }

    // =========================================================================
    // 4. Operation scope independence
    // =========================================================================
    @Test
    void testOperationScopeIndependence() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000005", new BigDecimal("500.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);
        String depositHash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        String withdrawHash = CoreRequestFingerprint.forWithdrawal(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("op-scoped-key");

        IdempotentCommandResult resDeposit = coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, depositHash, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Deposit",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "DEPOSIT-OK");
        });

        IdempotentCommandResult resWithdraw = coordinator.execute(userAId, CoreOperationType.WITHDRAWAL, key, withdrawHash, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.WITHDRAWAL,
                    CurrencyCode.LKR,
                    "Withdrawal",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "WITHDRAW-OK");
        });

        assertThat(resDeposit.replayed()).isFalse();
        assertThat(resWithdraw.replayed()).isFalse();
        assertThat(idempotencyRepository.findAll()).hasSize(2);
    }

    // =========================================================================
    // 5. Database collation case-insensitive key collision replays cached result
    // =========================================================================
    @Test
    void testDatabaseCollationCaseInsensitiveKeyCollisionReplaysCachedResult() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000006", new BigDecimal("500.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);

        IdempotencyKey lowerKey = IdempotencyKey.of("case-key-xyz");
        IdempotencyKey upperKey = IdempotencyKey.of("CASE-KEY-XYZ");

        AtomicInteger callCount = new AtomicInteger(0);
        IdempotentAction action = () -> {
            callCount.incrementAndGet();
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Case test deposit",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK-CASE");
        };

        // First call with lower-case key
        IdempotentCommandResult res1 = coordinator.execute(userAId, CoreOperationType.DEPOSIT, lowerKey, hash, action);
        assertThat(res1.replayed()).isFalse();
        assertThat(callCount.get()).isEqualTo(1);

        // Second call with UPPER-CASE key: MySQL utf8mb4_0900_ai_ci collides on UNIQUE index, replays
        IdempotentCommandResult res2 = coordinator.execute(userAId, CoreOperationType.DEPOSIT, upperKey, hash, action);
        assertThat(res2.replayed()).isTrue();
        assertThat(callCount.get()).isEqualTo(1);
        assertThat(res2.journalEntryId()).isEqualTo(res1.journalEntryId());
    }

    // =========================================================================
    // 6. Concurrent identical requests execute exactly once
    // =========================================================================
    @Test
    void testConcurrentIdenticalRequestsExecuteExactlyOnce() throws Exception {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000007", new BigDecimal("1000.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("concurrent-idem-key");

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger executions = new AtomicInteger(0);

        Callable<IdempotentCommandResult> task = () -> {
            barrier.await();
            return coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
                executions.incrementAndGet();
                try {
                    Thread.sleep(150); // Simulate work while holding row lock
                } catch (InterruptedException ignored) {}
                PostingCommand cmd = new PostingCommand(
                        JournalEntryType.DEPOSIT,
                        CurrencyCode.LKR,
                        "Concurrent deposit",
                        PostingActor.user(userAId),
                        LedgerChannel.WEB,
                        List.of(
                                new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                                new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                        )
                );
                PostingResult pr = postingEngine.post(cmd);
                return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "{\"concurrent\":true}");
            });
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<IdempotentCommandResult> f1 = executor.submit(task);
        Future<IdempotentCommandResult> f2 = executor.submit(task);

        IdempotentCommandResult r1 = f1.get(10, TimeUnit.SECONDS);
        IdempotentCommandResult r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one executed, one replayed
        assertThat(executions.get()).isEqualTo(1);
        assertThat(r1.replayed() ^ r2.replayed()).isTrue(); // One true, one false
        assertThat(r1.journalEntryId()).isEqualTo(r2.journalEntryId());
        assertThat(r1.responsePayload()).isEqualTo(r2.responsePayload());

        // Balance moved by exactly 100.0000
        Account accFinal = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accFinal.getBalance()).isEqualByComparingTo("1100.0000");

        // Exactly one journal entry, exactly one COMPLETED idempotency row
        assertThat(idempotencyRepository.findAll()).hasSize(1);
        assertThat(journalEntryRepository.count()).isEqualTo(1);
    }

    // =========================================================================
    // 7. First request failure rolls back claim and allows successful retry
    // =========================================================================
    @Test
    void testFirstRequestFailureRollsBackClaimAndAllowsSuccessfulRetry() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000008", new BigDecimal("1000.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("retry-after-failure-key");

        // First attempt fails during financial action
        assertThatThrownBy(() -> coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            throw new RuntimeException("Simulated financial failure in action");
        })).isInstanceOf(RuntimeException.class).hasMessageContaining("Simulated financial failure");

        // Verify claim was rolled back completely
        assertThat(idempotencyRepository.findAll()).isEmpty();
        Account accAfterFail = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accAfterFail.getBalance()).isEqualByComparingTo("1000.0000");

        // Second attempt with same key succeeds
        IdempotentCommandResult retryResult = coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Retry deposit",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "RETRY-OK");
        });

        assertThat(retryResult.replayed()).isFalse();
        Account accFinal = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accFinal.getBalance()).isEqualByComparingTo("1100.0000");
        assertThat(idempotencyRepository.findAll()).hasSize(1);
    }

    // =========================================================================
    // 8. Concurrent owner failure allows waiter to recover and execute
    // =========================================================================
    @Test
    void testConcurrentOwnerFailureAllowsWaiterToRecoverAndExecute() throws Exception {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000009", new BigDecimal("1000.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("owner-fail-waiter-key");

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch ownerFailedLatch = new CountDownLatch(1);

        // Thread 1: gets claim, sleeps, then throws exception causing rollback
        Callable<IdempotentCommandResult> task1 = () -> {
            barrier.await();
            return coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
                ownerFailedLatch.countDown();
                throw new RuntimeException("Owner deliberately failing");
            });
        };

        // Thread 2: tries same key, waits on row lock, then recovers after Thread 1 rolls back!
        Callable<IdempotentCommandResult> task2 = () -> {
            barrier.await();
            Thread.sleep(50); // Ensure Thread 1 gets in first
            return coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
                PostingCommand cmd = new PostingCommand(
                        JournalEntryType.DEPOSIT,
                        CurrencyCode.LKR,
                        "Waiter deposit",
                        PostingActor.user(userAId),
                        LedgerChannel.WEB,
                        List.of(
                                new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                                new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                        )
                );
                PostingResult pr = postingEngine.post(cmd);
                return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "WAITER-OK");
            });
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<IdempotentCommandResult> f1 = executor.submit(task1);
        Future<IdempotentCommandResult> f2 = executor.submit(task2);

        // f1 fails
        assertThatThrownBy(() -> f1.get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(RuntimeException.class);

        // f2 succeeds
        IdempotentCommandResult res2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(res2.replayed()).isFalse();
        assertThat(res2.responsePayload()).isEqualTo("WAITER-OK");

        // Exactly one balance increase
        Account accFinal = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(accFinal.getBalance()).isEqualByComparingTo("1100.0000");
        assertThat(idempotencyRepository.findAll()).hasSize(1);
    }

    // =========================================================================
    // 9. Failure on completion update rolls back entire transaction
    // =========================================================================
    @Test
    void testFailureOnCompletionUpdateRollsBackEntireTransaction() throws Exception {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000010", new BigDecimal("1000.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("complete-fail-key");

        // Install DB trigger rejecting transition to COMPLETED
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TRIGGER trg_test_fail_idem_complete " +
                    "BEFORE UPDATE ON core_transaction_idempotency " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  IF NEW.state = 'COMPLETED' THEN " +
                    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Simulated completion update failure'; " +
                    "  END IF; " +
                    "END"
            );
        }

        try {
            assertThatThrownBy(() -> coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
                PostingCommand cmd = new PostingCommand(
                        JournalEntryType.DEPOSIT,
                        CurrencyCode.LKR,
                        "Will rollback",
                        PostingActor.user(userAId),
                        LedgerChannel.WEB,
                        List.of(
                                new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                                new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                        )
                );
                PostingResult pr = postingEngine.post(cmd);
                return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK");
            })).hasMessageContaining("Simulated completion update failure");

            // Entire outer transaction must have rolled back
            Account accAfter = accountRepository.findById(acc.getAccountId()).orElseThrow();
            assertThat(accAfter.getBalance()).isEqualByComparingTo("1000.0000");
            assertThat(journalEntryRepository.count()).isEqualTo(0);
            assertThat(journalPostingRepository.count()).isEqualTo(0);
            assertThat(transactionRepository.count()).isEqualTo(0);
            assertThat(idempotencyRepository.findAll()).isEmpty();
        } finally {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_idem_complete");
            }
        }
    }

    // =========================================================================
    // 10. Manually committed PROCESSING row throws IdempotencyInProgressException
    // =========================================================================
    @Test
    void testManuallyCommittedProcessingRowThrowsInProgressException() throws Exception {
        IdempotencyKey key = IdempotencyKey.of("stalled-processing-key");
        String hash = CoreRequestFingerprint.sha256Hex("some-dummy-payload");

        // Manually insert a PROCESSING row using raw JDBC to avoid Spring TransactionRequiredException
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO core_transaction_idempotency " +
                     "(id, user_id, operation_type, client_key, request_hash, state, created_at) " +
                     "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, ?, 'PROCESSING', NOW(6))")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, userAId.toString());
            ps.setString(3, CoreOperationType.DEPOSIT.name());
            ps.setString(4, key.getValue());
            ps.setString(5, hash);
            ps.executeUpdate();
        }

        AtomicInteger actionCalled = new AtomicInteger(0);
        assertThatThrownBy(() -> coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            actionCalled.incrementAndGet();
            return IdempotentCommandResult.newlyExecuted(UUID.randomUUID(), 200, "OK");
        })).isInstanceOf(IdempotencyInProgressException.class)
           .hasMessageContaining("in progress or stalled");

        assertThat(actionCalled.get()).isEqualTo(0);
    }

    // =========================================================================
    // 11. Foreign-key result link and cached response exactness
    // =========================================================================
    @Test
    void testJournalEntryForeignKeyLinkAndCachedPayloadExactness() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000011", new BigDecimal("500.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();

        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("exact-cache-key");
        String exactJsonPayload = "{\"operation\":\"DEPOSIT\",\"accountId\":\"" + acc.getAccountId() + "\",\"currency\":\"LKR\",\"amount\":50.00}";

        IdempotentCommandResult res1 = coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Exact payload test",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, exactJsonPayload);
        });

        // Verify FK link in database
        CoreTransactionIdempotency claim = idempotencyRepository.findAll().get(0);
        assertThat(claim.getEntryId()).isNotNull();
        assertThat(claim.getEntryId()).isEqualTo(res1.journalEntryId());
        assertThat(journalEntryRepository.findById(claim.getEntryId())).isPresent();

        // Replay and verify byte-for-byte exact payload
        IdempotentCommandResult res2 = coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            throw new AssertionError("Should not execute");
        });

        assertThat(res2.replayed()).isTrue();
        assertThat(res2.journalEntryId()).isEqualTo(res1.journalEntryId());
        assertThat(res2.httpStatusCode()).isEqualTo(201);
        assertThat(res2.responsePayload()).isEqualTo(exactJsonPayload);
    }

    // =========================================================================
    // 12. PostingEngine still works independently without idempotency
    // =========================================================================
    @Test
    void testPostingEngineStillWorksIndependentlyWithoutIdempotency() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000012", new BigDecimal("500.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("25.00", CurrencyCode.LKR);

        PostingCommand cmd = new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Direct posting without idempotency",
                PostingActor.user(userAId),
                LedgerChannel.WEB,
                List.of(
                        new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                        new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                )
        );

        PostingResult result = postingEngine.post(cmd);
        assertThat(result).isNotNull();
        assertThat(result.getEntryId()).isNotNull();

        Account updated = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo("525.0000");

        // Idempotency table remains untouched
        assertThat(idempotencyRepository.findAll()).isEmpty();
    }

    // =========================================================================
    // 13. Reconciliation remains clean after idempotent operations
    // =========================================================================
    @Test
    void testReconciliationRemainsCleanAfterIdempotentOperations() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000013", BigDecimal.ZERO);
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("75.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("reconciliation-key");

        coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Reconciliation test deposit",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK");
        });

        // Replay call
        coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            throw new AssertionError("Should not execute");
        });

        LedgerReconciliationResult rec = reconciliationService.reconcileAll();
        assertThat(rec.isClean()).isTrue();
        assertThat(rec.getDiscrepancies()).isEmpty();
    }

    // =========================================================================
    // 14. Replay does not create additional legacy projection rows
    // =========================================================================
    @Test
    void testReplayDoesNotCreateAdditionalLegacyProjectionRows() {
        Account acc = createCustomerAccount(testCustomerA, "ACC9000000014", new BigDecimal("500.0000"));
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(acc.getAccountId(), CurrencyCode.LKR, amount);
        IdempotencyKey key = IdempotencyKey.of("projection-check-key");

        coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
            PostingCommand cmd = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Projection check deposit",
                    PostingActor.user(userAId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultCashLedgerAccount.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                            new PostingInstruction(la.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                    )
            );
            PostingResult pr = postingEngine.post(cmd);
            return IdempotentCommandResult.newlyExecuted(pr.getEntryId(), 201, "OK");
        });

        List<Transaction> txsAfter1 = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(acc.getAccountId());
        assertThat(txsAfter1).hasSize(1);

        // Replay 3 times
        for (int i = 0; i < 3; i++) {
            coordinator.execute(userAId, CoreOperationType.DEPOSIT, key, hash, () -> {
                throw new AssertionError("Should not execute");
            });
        }

        List<Transaction> txsAfterReplays = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(acc.getAccountId());
        assertThat(txsAfterReplays).hasSize(1);
    }
}
