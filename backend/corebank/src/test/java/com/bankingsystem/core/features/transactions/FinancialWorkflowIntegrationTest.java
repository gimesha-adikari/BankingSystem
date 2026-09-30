package com.bankingsystem.core.features.transactions;

import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationService;
import com.bankingsystem.core.features.ledger.application.PostingCommand;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.application.PostingInstruction;
import com.bankingsystem.core.features.ledger.domain.CurrencyCode;
import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.ledger.domain.LedgerAccount;
import com.bankingsystem.core.features.ledger.domain.LedgerAccountClass;
import com.bankingsystem.core.features.ledger.domain.LedgerAccountStatus;
import com.bankingsystem.core.features.ledger.domain.LedgerChannel;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;
import com.bankingsystem.core.features.ledger.domain.PostingActor;
import com.bankingsystem.core.features.ledger.domain.PostingDirection;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.DepositReceipt;
import com.bankingsystem.core.features.transactions.application.DepositWorkflow;
import com.bankingsystem.core.features.transactions.application.TransferReceipt;
import com.bankingsystem.core.features.transactions.application.TransferWorkflow;
import com.bankingsystem.core.features.transactions.application.WithdrawalReceipt;
import com.bankingsystem.core.features.transactions.application.WithdrawalWorkflow;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreOperationType;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreTransactionIdempotency;
import com.bankingsystem.core.features.transactions.idempotency.domain.IdempotencyConflictException;
import com.bankingsystem.core.features.transactions.idempotency.domain.repository.CoreTransactionIdempotencyRepository;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import com.bankingsystem.core.modules.common.enums.Gender;
import com.bankingsystem.core.modules.common.enums.Status;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real-MySQL proof for the internal 4B-7 customer financial workflows.
 *
 * <p>The test uses trusted UUIDs in place of HTTP authentication. It therefore
 * exercises the server-side ownership seam without exposing or reimplementing
 * controller authentication.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinancialWorkflowIntegrationTest {

    @Autowired
    private DepositWorkflow depositWorkflow;

    @Autowired
    private WithdrawalWorkflow withdrawalWorkflow;

    @Autowired
    private TransferWorkflow transferWorkflow;

    @Autowired
    private PostingEngine postingEngine;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalPostingRepository journalPostingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CoreTransactionIdempotencyRepository idempotencyRepository;

    @Autowired
    private LedgerReconciliationService reconciliationService;

    @Autowired
    private DataSource dataSource;

    private LedgerAccount vaultCash;

    @BeforeEach
    void setUp() throws Exception {
        cleanTestData();
        vaultCash = ensureVault();
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanTestData();
    }

    @Test
    void depositExecutesOnceReplaysCachedReceiptAndProjectsSynchronously() {
        TestCustomer alice = createCustomer("alice");
        Account account = createAccount(alice, "100.00");
        long journalsBefore = journalEntryRepository.count();
        long postingsBefore = journalPostingRepository.count();
        long transactionsBefore = transactionRepository.count();

        DepositReceipt first = depositWorkflow.deposit(
                alice.user().getUserId(), account.getAccountId(), "50.00", "dep-001");
        DepositReceipt replay = depositWorkflow.deposit(
                alice.user().getUserId(), account.getAccountId(), "50", "DEP-001");

        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.journalEntryId()).isEqualTo(first.journalEntryId());
        assertThat(replay.resultingBalance()).isEqualByComparingTo("150.0000");
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("150.0000");
        assertThat(journalEntryRepository.count()).isEqualTo(journalsBefore + 1);
        assertThat(journalPostingRepository.count()).isEqualTo(postingsBefore + 2);
        assertThat(transactionRepository.count()).isEqualTo(transactionsBefore + 1);
        var depositEntry = journalEntryRepository.findById(first.journalEntryId()).orElseThrow();
        assertThat(depositEntry.getEntryType()).isEqualTo(JournalEntryType.DEPOSIT);
        assertThat(depositEntry.getActorType())
                .isEqualTo(com.bankingsystem.core.features.ledger.domain.LedgerActorType.USER);
        assertThat(depositEntry.getInitiatedByUserId()).isEqualTo(alice.user().getUserId());
        assertThat(depositEntry.getChannel()).isEqualTo(LedgerChannel.WEB);
        assertThat(depositEntry.getDescription()).isEqualTo("Deposit");
        assertThat(journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(first.journalEntryId()))
                .extracting(com.bankingsystem.core.features.ledger.domain.JournalPosting::getDirection)
                .containsExactly(PostingDirection.DEBIT, PostingDirection.CREDIT);
        assertThat(idempotencyRepository.findByUserIdAndOperationTypeAndClientKey(
                alice.user().getUserId(), CoreOperationType.DEPOSIT, "dep-001"))
                .get()
                .satisfies(claim -> {
                    assertThat(claim.getEntryId()).isEqualTo(first.journalEntryId());
                    assertThat(claim.getResponseStatusCode()).isEqualTo(200);
                    assertThat(claim.getResponsePayload()).contains("150.0000");
                });
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();

        List<Transaction> history = transactionRepository
                .findByAccountAccountIdOrderByCreatedAtDesc(account.getAccountId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(history.get(0).getJournalEntryId()).isEqualTo(first.journalEntryId());
    }

    @Test
    void depositConflictAndBolaAttemptsDoNotMoveMoneyOrLeaveClaims() {
        TestCustomer alice = createCustomer("alice");
        TestCustomer bob = createCustomer("bob");
        Account aliceAccount = createAccount(alice, "100.00");

        depositWorkflow.deposit(alice.user().getUserId(), aliceAccount.getAccountId(), "50.00", "dep-conflict");
        assertThatThrownBy(() -> depositWorkflow.deposit(
                alice.user().getUserId(), aliceAccount.getAccountId(), "51.00", "dep-conflict"))
                .isInstanceOf(IdempotencyConflictException.class);

        assertThatThrownBy(() -> depositWorkflow.deposit(
                bob.user().getUserId(), aliceAccount.getAccountId(), "10.00", "bola-deposit"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_ACCOUNT_ACCESS_DENIED"));
        assertThatThrownBy(() -> withdrawalWorkflow.withdraw(
                bob.user().getUserId(), aliceAccount.getAccountId(), "10.00", "bola-withdrawal"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_ACCOUNT_ACCESS_DENIED"));

        assertThat(accountRepository.findById(aliceAccount.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("150.0000");
        assertThat(idempotencyRepository.findByUserIdAndOperationTypeAndClientKey(
                bob.user().getUserId(), CoreOperationType.DEPOSIT, "bola-deposit")).isEmpty();
        assertThat(idempotencyRepository.findByUserIdAndOperationTypeAndClientKey(
                bob.user().getUserId(), CoreOperationType.WITHDRAWAL, "bola-withdrawal")).isEmpty();
        assertThat(reconciliationService.reconcileAccount(aliceAccount.getAccountId()).isClean()).isTrue();
    }

    @Test
    void withdrawalExecutesOnceAndInsufficientFundsRetryDoesNotPoisonKey() {
        TestCustomer alice = createCustomer("alice");
        Account account = createAccount(alice, "50.00");

        assertThatThrownBy(() -> withdrawalWorkflow.withdraw(
                alice.user().getUserId(), account.getAccountId(), "60.00", "retry-withdrawal"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_INSUFFICIENT_FUNDS"));
        assertThat(idempotencyRepository.findByUserIdAndOperationTypeAndClientKey(
                alice.user().getUserId(), CoreOperationType.WITHDRAWAL, "retry-withdrawal")).isEmpty();

        depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "20.00", "fund-retry");
        WithdrawalReceipt first = withdrawalWorkflow.withdraw(
                alice.user().getUserId(), account.getAccountId(), "60.00", "retry-withdrawal");
        WithdrawalReceipt replay = withdrawalWorkflow.withdraw(
                alice.user().getUserId(), account.getAccountId(), "60", "retry-withdrawal");

        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(first.resultingBalance()).isEqualByComparingTo("10.0000");
        assertThat(replay.resultingBalance()).isEqualByComparingTo("10.0000");
        assertThat(replay.journalEntryId()).isEqualTo(first.journalEntryId());
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void concurrentDifferentKeyWithdrawalsAllowOnlyOneEconomicCommit() throws Exception {
        TestCustomer alice = createCustomer("alice");
        Account account = createAccount(alice, "100.00");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (String key : List.of("concurrent-a", "concurrent-b")) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                    try {
                        return withdrawalWorkflow.withdraw(
                                alice.user().getUserId(), account.getAccountId(), "80.00", key);
                    } catch (Throwable failure) {
                        return failure;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> outcomes = futures.stream().map(this::getOutcome).toList();
            assertThat(outcomes).filteredOn(WithdrawalReceipt.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(BusinessException.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(BusinessException.class::isInstance)
                    .extracting(value -> ((BusinessException) value).getCode())
                    .containsExactly("ERR_INSUFFICIENT_FUNDS");
        } finally {
            executor.shutdownNow();
        }

        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("20.0000");
        assertThat(journalEntryRepository.count()).isEqualTo(2);
        assertThat(idempotencyRepository.findAll()).hasSize(1);
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void transferAllowsUnownedDestinationReplaysAndProjectsBothSides() {
        TestCustomer alice = createCustomer("alice");
        TestCustomer bob = createCustomer("bob");
        Account source = createAccount(alice, "100.00");
        Account destination = createAccount(bob, "20.00");
        long journalsBefore = journalEntryRepository.count();
        long postingsBefore = journalPostingRepository.count();
        long transactionsBefore = transactionRepository.count();

        TransferReceipt first = transferWorkflow.transfer(
                alice.user().getUserId(), source.getAccountId(), destination.getAccountId(), "25.00", "transfer-001");
        TransferReceipt replay = transferWorkflow.transfer(
                alice.user().getUserId(), source.getAccountId(), destination.getAccountId(), "25", "TRANSFER-001");

        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(first.sourceResultingBalance()).isEqualByComparingTo("75.0000");
        assertThat(first.destinationResultingBalance()).isEqualByComparingTo("45.0000");
        assertThat(replay.sourceResultingBalance()).isEqualByComparingTo("75.0000");
        assertThat(replay.destinationResultingBalance()).isEqualByComparingTo("45.0000");
        assertThat(replay.journalEntryId()).isEqualTo(first.journalEntryId());
        var transferEntry = journalEntryRepository.findById(first.journalEntryId()).orElseThrow();
        assertThat(transferEntry.getEntryType()).isEqualTo(JournalEntryType.TRANSFER);
        assertThat(transferEntry.getActorType())
                .isEqualTo(com.bankingsystem.core.features.ledger.domain.LedgerActorType.USER);
        assertThat(transferEntry.getInitiatedByUserId()).isEqualTo(alice.user().getUserId());
        assertThat(transferEntry.getChannel()).isEqualTo(LedgerChannel.WEB);
        assertThat(transferEntry.getDescription()).isEqualTo("Transfer");
        assertThat(journalEntryRepository.count()).isEqualTo(journalsBefore + 1);
        assertThat(journalPostingRepository.count()).isEqualTo(postingsBefore + 2);
        assertThat(transactionRepository.count()).isEqualTo(transactionsBefore + 2);
        assertThat(transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(source.getAccountId()))
                .extracting(Transaction::getType).contains(Transaction.TransactionType.TRANSFER_OUT);
        assertThat(transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(destination.getAccountId()))
                .extracting(Transaction::getType).contains(Transaction.TransactionType.TRANSFER_IN);
        assertThat(idempotencyRepository.findByUserIdAndOperationTypeAndClientKey(
                alice.user().getUserId(), CoreOperationType.TRANSFER, "transfer-001")).isPresent();
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    @Test
    void transferSourceOwnershipAndSelfTransferAreRejectedBeforeCompletion() {
        TestCustomer alice = createCustomer("alice");
        TestCustomer bob = createCustomer("bob");
        Account source = createAccount(alice, "100.00");
        Account destination = createAccount(bob, "20.00");

        assertThatThrownBy(() -> transferWorkflow.transfer(
                bob.user().getUserId(), source.getAccountId(), destination.getAccountId(), "10.00", "bola-transfer"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_ACCOUNT_ACCESS_DENIED"));
        assertThatThrownBy(() -> transferWorkflow.transfer(
                alice.user().getUserId(), source.getAccountId(), source.getAccountId(), "10.00", "self-transfer"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_SELF_TRANSFER"));

        assertThat(idempotencyRepository.findAll()).isEmpty();
        assertThat(accountRepository.findById(source.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
        assertThat(accountRepository.findById(destination.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("20.0000");
    }

    @Test
    void inactiveAccountsAndCorruptCustomerMappingFailBeforePosting() {
        TestCustomer alice = createCustomer("alice");
        TestCustomer bob = createCustomer("bob");
        Account account = createAccount(alice, "100.00");
        Account destination = createAccount(bob, "20.00");

        for (AccountStatus status : List.of(AccountStatus.FROZEN, AccountStatus.CLOSED)) {
            account.setAccountStatus(status);
            accountRepository.saveAndFlush(account);
            String suffix = status.name().toLowerCase();
            assertThatThrownBy(() -> depositWorkflow.deposit(
                    alice.user().getUserId(), account.getAccountId(), "10.00", "status-dep-" + suffix))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("ERR_ACCOUNT_NOT_ACTIVE"));
            assertThatThrownBy(() -> withdrawalWorkflow.withdraw(
                    alice.user().getUserId(), account.getAccountId(), "10.00", "status-wd-" + suffix))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("ERR_ACCOUNT_NOT_ACTIVE"));
        }

        account.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.saveAndFlush(account);
        for (AccountStatus status : List.of(AccountStatus.FROZEN, AccountStatus.CLOSED)) {
            destination.setAccountStatus(status);
            accountRepository.saveAndFlush(destination);
            String suffix = status.name().toLowerCase();
            assertThatThrownBy(() -> transferWorkflow.transfer(
                    alice.user().getUserId(), account.getAccountId(), destination.getAccountId(),
                    "10.00", "status-transfer-" + suffix))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("ERR_ACCOUNT_NOT_ACTIVE"));
        }

        destination.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.saveAndFlush(destination);
        LedgerAccount customerLedger = ledgerAccountRepository
                .findByCustomerAccountId(account.getAccountId()).orElseThrow();
        customerLedger.setAccountClass(LedgerAccountClass.ASSET);
        ledgerAccountRepository.saveAndFlush(customerLedger);
        assertThatThrownBy(() -> withdrawalWorkflow.withdraw(
                alice.user().getUserId(), account.getAccountId(), "10.00", "corrupt-mapping"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_CORRUPT_LEDGER_MAPPING"));

        assertThat(idempotencyRepository.findAll()).isEmpty();
        assertThat(reconciliationService.reconcileAll().isClean()).isFalse();
    }

    @Test
    void sameKeyIsIndependentAcrossOperationsAndUsers() {
        TestCustomer alice = createCustomer("alice");
        TestCustomer bob = createCustomer("bob");
        Account aliceAccount = createAccount(alice, "0.00");
        Account bobAccount = createAccount(bob, "0.00");

        depositWorkflow.deposit(alice.user().getUserId(), aliceAccount.getAccountId(), "50.00", "shared-001");
        withdrawalWorkflow.withdraw(alice.user().getUserId(), aliceAccount.getAccountId(), "10.00", "shared-001");
        depositWorkflow.deposit(bob.user().getUserId(), bobAccount.getAccountId(), "30.00", "shared-001");

        assertThat(idempotencyRepository.findAll()).hasSize(3);
        assertThat(aliceAccount.getAccountId()).isNotEqualTo(bobAccount.getAccountId());
        assertThat(accountRepository.findById(aliceAccount.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("40.0000");
        assertThat(accountRepository.findById(bobAccount.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("30.0000");
    }

    @Test
    void replayUsesOriginalReceiptAfterLaterBalanceChange() {
        TestCustomer alice = createCustomer("alice");
        Account account = createAccount(alice, "100.00");

        DepositReceipt original = depositWorkflow.deposit(
                alice.user().getUserId(), account.getAccountId(), "50.00", "historical-receipt");
        depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "20.00", "later-activity");
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("170.0000");

        DepositReceipt replay = depositWorkflow.deposit(
                alice.user().getUserId(), account.getAccountId(), "50", "historical-receipt");
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.journalEntryId()).isEqualTo(original.journalEntryId());
        assertThat(replay.resultingBalance()).isEqualByComparingTo("150.0000");
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("170.0000");
        assertThat(journalEntryRepository.count()).isEqualTo(3);
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void missingVaultRollsBackClaimAndProducesNoFinancialRows() {
        TestCustomer alice = createCustomer("alice");
        Account account = createAccount(alice, "0.00");
        ledgerAccountRepository.deleteById(vaultCash.getLedgerAccountId());
        ledgerAccountRepository.flush();

        assertThatThrownBy(() -> depositWorkflow.deposit(
                alice.user().getUserId(), account.getAccountId(), "10.00", "missing-vault"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("ERR_VAULT_ACCOUNT_NOT_FOUND"));
        assertThat(idempotencyRepository.findAll()).isEmpty();
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(journalPostingRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.0000");
    }

    private Object getOutcome(Future<Object> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Concurrent workflow test interrupted", ex);
        } catch (ExecutionException ex) {
            return ex.getCause();
        } catch (Exception ex) {
            return ex;
        }
    }

    private TestCustomer createCustomer(String label) {
        Role role = roleRepository.findByRoleNameIgnoreCase("CUSTOMER")
                .orElseGet(() -> {
                    Role created = new Role();
                    created.setRoleName("CUSTOMER");
                    created.setDescription("Customer role for workflow tests");
                    return roleRepository.saveAndFlush(created);
                });

        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername("4b7w-" + label + "-" + suffix);
        user.setPasswordHash("$2a$10$workflow-test-hash");
        user.setEmail("4b7-workflow-" + label + "-" + suffix + "@example.test");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user = userRepository.saveAndFlush(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFirstName(label);
        customer.setLastName("Workflow");
        customer.setGender(Gender.OTHER);
        customer.setEmail(user.getEmail());
        customer.setPhone("+9477000" + suffix.substring(0, 5));
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setStatus(Status.ACTIVE);
        customer.setAddress("Workflow test address");
        customer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer = customerRepository.saveAndFlush(customer);
        return new TestCustomer(user, customer);
    }

    private Account createAccount(TestCustomer owner, String initialDeposit) {
        Account account = new Account();
        account.setAccountNumber("4B7W-" + UUID.randomUUID().toString().replace("-", ""));
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("0.0000"));
        account.setCurrency("LKR");
        account.setCustomer(owner.customer());
        account.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account = accountRepository.saveAndFlush(account);

        ledgerAccountRepository.saveAndFlush(new LedgerAccount(
                UUID.randomUUID(), account.getAccountId(), null,
                LedgerAccountClass.LIABILITY, "LKR", LedgerAccountStatus.ACTIVE,
                LocalDateTime.now(ZoneOffset.UTC)));

        if (new BigDecimal(initialDeposit).compareTo(BigDecimal.ZERO) > 0) {
            seedDeposit(account, initialDeposit);
        }
        return account;
    }

    private void seedDeposit(Account account, String amountText) {
        LedgerAccount customerLedger = ledgerAccountRepository
                .findByCustomerAccountId(account.getAccountId()).orElseThrow();
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput(amountText, CurrencyCode.LKR);
        postingEngine.post(new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Test seed",
                PostingActor.system("4B7_TEST_SEED"),
                LedgerChannel.SYSTEM,
                List.of(
                        new PostingInstruction(vaultCash.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                        new PostingInstruction(customerLedger.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                )
        ));
    }

    private LedgerAccount ensureVault() {
        return ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR")
                .orElseGet(() -> ledgerAccountRepository.saveAndFlush(new LedgerAccount(
                        UUID.randomUUID(), null, "SYSTEM_VAULT_CASH:LKR",
                        LedgerAccountClass.ASSET, "LKR", LedgerAccountStatus.ACTIVE,
                        LocalDateTime.now(ZoneOffset.UTC))));
    }

    private void cleanTestData() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.execute("DELETE FROM core_transaction_idempotency");
            statement.execute("DELETE FROM transactions");
            statement.execute("DELETE FROM journal_postings");
            statement.execute("DELETE FROM journal_entries");
            statement.execute("DELETE FROM ledger_accounts WHERE customer_account_id IS NOT NULL");
            statement.execute("DELETE FROM accounts");
            statement.execute("DELETE FROM customers WHERE email LIKE '4b7-workflow-%'");
            statement.execute("DELETE FROM users WHERE email LIKE '4b7-workflow-%'");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private record TestCustomer(User user, Customer customer) {
    }
}
