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
import com.bankingsystem.core.features.ledger.application.ReversalPostingResult;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.*;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.modules.common.enums.*;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

/** Real-MySQL proof for the internal full-reversal workflow. */
@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReversalWorkflowIntegrationTest {

    private static final long TIMEOUT_SECONDS = 30;
    private static final String USER_PREFIX = "4b10a-";

    @Autowired private ReversalWorkflow reversalWorkflow;
    @Autowired private DepositWorkflow depositWorkflow;
    @Autowired private WithdrawalWorkflow withdrawalWorkflow;
    @Autowired private TransferWorkflow transferWorkflow;
    @Autowired private PostingEngine postingEngine;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private LedgerAccountRepository ledgerAccountRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalPostingRepository journalPostingRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private LedgerReconciliationService reconciliationService;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @SpyBean private PostingEngine postingEngineSpy;

    private LedgerAccount vault;

    @BeforeEach
    void setUp() throws Exception {
        cleanTestData();
        vault = ensureVault();
    }

    @AfterEach
    void tearDown() throws Exception {
        reset(postingEngineSpy);
        restoreVault();
        cleanTestData();
    }

    @Test
    void depositReversalCreatesInverseAndLeavesOriginalImmutable() {
        TestCustomer alice = customer("deposit");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "100.00", "4b10-deposit");
        JournalEntry before = journalEntryRepository.findById(original.journalEntryId()).orElseThrow();
        List<JournalPosting> beforePostings = journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(before.getEntryId());
        long journals = journalEntryRepository.count();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();

        ReversalReceipt receipt = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "customer correction");

        assertThat(receipt.replayed()).isFalse();
        assertThat(receipt.amount()).isEqualByComparingTo("100.0000");
        assertThat(receipt.resultingCustomerBalances().get(account.getAccountId())).isEqualByComparingTo("0.0000");
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("0.0000");
        assertThat(journalEntryRepository.count()).isEqualTo(journals + 1);
        assertThat(journalPostingRepository.count()).isEqualTo(postings + 2);
        assertThat(transactionRepository.count()).isEqualTo(projections + 1);

        JournalEntry originalAfter = journalEntryRepository.findById(before.getEntryId()).orElseThrow();
        assertThat(originalAfter.getEntryType()).isEqualTo(before.getEntryType());
        assertThat(originalAfter.getDescription()).isEqualTo(before.getDescription());
        assertThat(originalAfter.getReversalOfEntryId()).isNull();
        assertThat(journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(before.getEntryId()))
                .extracting(JournalPosting::getDirection).containsExactlyElementsOf(
                        beforePostings.stream().map(JournalPosting::getDirection).toList());

        JournalEntry reversal = journalEntryRepository.findById(receipt.reversalJournalEntryId()).orElseThrow();
        assertThat(reversal.getEntryType()).isEqualTo(JournalEntryType.REVERSAL);
        assertThat(reversal.getReversalOfEntryId()).isEqualTo(before.getEntryId());
        assertThat(reversal.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(reversal.getActorType()).isEqualTo(LedgerActorType.USER);
        assertThat(reversal.getInitiatedByUserId()).isEqualTo(alice.user().getUserId());
        assertThat(reversal.getSystemActorId()).isNull();
        assertThat(reversal.getChannel()).isEqualTo(LedgerChannel.WEB);
        assertThat(reversal.getDescription()).startsWith("Reversal of " + before.getEntryReference() + ":");

        List<JournalPosting> inverse = journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(reversal.getEntryId());
        assertThat(inverse).hasSize(2);
        for (int i = 0; i < beforePostings.size(); i++) {
            assertThat(inverse.get(i).getLedgerAccountId()).isEqualTo(beforePostings.get(i).getLedgerAccountId());
            assertThat(inverse.get(i).getAmount()).isEqualByComparingTo(beforePostings.get(i).getAmount());
            assertThat(inverse.get(i).getDirection()).isNotEqualTo(beforePostings.get(i).getDirection());
        }
        Transaction projection = transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversal.getEntryId()).get(0);
        assertThat(projection.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(projection.getAmount()).isEqualByComparingTo("100.0000");
        assertThat(projection.getBalanceAfter()).isEqualByComparingTo("0.0000");
        assertThat(projection.getCreatedAt()).isEqualTo(reversal.getPostedAt());
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void withdrawalReversalRestoresBalanceAndProjectsDeposit() {
        TestCustomer alice = customer("withdrawal");
        Account account = account(alice, "100");
        WithdrawalReceipt original = withdrawalWorkflow.withdraw(alice.user().getUserId(), account.getAccountId(), "40.00", "4b10-withdrawal");
        ReversalReceipt reversal = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "cash correction");

        assertThat(reversal.amount()).isEqualByComparingTo("40.0000");
        assertThat(reversal.resultingCustomerBalances().get(account.getAccountId())).isEqualByComparingTo("100.0000");
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("100.0000");
        List<Transaction> rows = transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversal.reversalJournalEntryId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo("40.0000");
        assertThat(rows.get(0).getBalanceAfter()).isEqualByComparingTo("100.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void transferReversalInvertsBothCustomerLegsAndProjectionTypes() {
        TestCustomer alice = customer("transfer-source");
        TestCustomer bob = customer("transfer-destination");
        Account source = account(alice, "100");
        Account destination = account(bob, "20");
        TransferReceipt original = transferWorkflow.transfer(alice.user().getUserId(), source.getAccountId(), destination.getAccountId(), "25.00", "4b10-transfer");

        ReversalReceipt reversal = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "transfer correction");

        assertThat(accountRepository.findById(source.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("100.0000");
        assertThat(accountRepository.findById(destination.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("20.0000");
        List<Transaction> rows = transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversal.reversalJournalEntryId());
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(Transaction::getType).containsExactlyInAnyOrder(
                Transaction.TransactionType.TRANSFER_IN, Transaction.TransactionType.TRANSFER_OUT);
        assertThat(rows).filteredOn(r -> r.getAccount().getAccountId().equals(source.getAccountId()))
                .singleElement().satisfies(r -> {
                    assertThat(r.getType()).isEqualTo(Transaction.TransactionType.TRANSFER_IN);
                    assertThat(r.getBalanceAfter()).isEqualByComparingTo("100.0000");
                });
        assertThat(rows).filteredOn(r -> r.getAccount().getAccountId().equals(destination.getAccountId()))
                .singleElement().satisfies(r -> {
                    assertThat(r.getType()).isEqualTo(Transaction.TransactionType.TRANSFER_OUT);
                    assertThat(r.getBalanceAfter()).isEqualByComparingTo("20.0000");
                });
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    @Test
    void sameOriginalConcurrentReversalsCommitOnceAndReplayStable() throws Exception {
        TestCustomer alice = customer("concurrent");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "100.00", "4b10-concurrent-original");
        long reversalJournals = journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL).count();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<ReversalReceipt>> futures = List.of(
                    executor.submit(() -> concurrentReverse(barrier, alice.user().getUserId(), original.journalEntryId(), "first")),
                    executor.submit(() -> concurrentReverse(barrier, alice.user().getUserId(), original.journalEntryId(), "second")));
            ReversalReceipt first = futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            ReversalReceipt second = futures.get(1).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(first.reversalJournalEntryId()).isEqualTo(second.reversalJournalEntryId());
            assertThat(List.of(first.replayed(), second.replayed())).contains(false, true);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL).count())
                .isEqualTo(reversalJournals + 1);
        UUID reversalId = journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)
                .findFirst().orElseThrow().getEntryId();
        assertThat(journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(reversalId)).hasSize(2);
        assertThat(transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversalId)).hasSize(1);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("0.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();

        ReversalReceipt replay = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "later reason ignored");
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.reversalJournalEntryId()).isEqualTo(reversalId);
    }

    @Test
    void reversalAndWithdrawalContentionHasOneSerializableWinner() throws Exception {
        TestCustomer alice = customer("operation-contention");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "100.00", "4b10-contention-original");
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Object>> futures;
        try {
            futures = List.of(
                    executor.submit(() -> {
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        try {
                            return reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "contention reversal");
                        } catch (Throwable failure) {
                            return failure;
                        }
                    }),
                    executor.submit(() -> {
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        try {
                            return withdrawalWorkflow.withdraw(alice.user().getUserId(), account.getAccountId(), "80.00", "4b10-contention-withdrawal");
                        } catch (Throwable failure) {
                            return failure;
                        }
                    }));
            Object first = futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Object second = futures.get(1).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(List.of(first, second)).filteredOn(value -> value instanceof ReversalReceipt).hasSizeLessThanOrEqualTo(1);
            assertThat(List.of(first, second)).filteredOn(value -> value instanceof WithdrawalReceipt).hasSizeLessThanOrEqualTo(1);
            assertThat(List.of(first, second)).filteredOn(value -> value instanceof BusinessException).hasSize(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        BigDecimal finalBalance = accountRepository.findById(account.getAccountId()).orElseThrow().getBalance();
        assertThat(finalBalance).isIn(new BigDecimal("0.0000"), new BigDecimal("20.0000"));
        assertThat(finalBalance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void transferReversalContentionUsesCanonicalAccountLockOrder() throws Exception {
        TestCustomer alice = customer("transfer-contention-source");
        TestCustomer bob = customer("transfer-contention-destination");
        Account source = account(alice, "100");
        Account destination = account(bob, "0");
        TransferReceipt original = transferWorkflow.transfer(alice.user().getUserId(), source.getAccountId(), destination.getAccountId(), "25.00", "4b10-transfer-contention-original");
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ReversalReceipt> reversal = executor.submit(() -> {
                barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "transfer contention reversal");
            });
            Future<TransferReceipt> competingTransfer = executor.submit(() -> {
                barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return transferWorkflow.transfer(alice.user().getUserId(), source.getAccountId(), destination.getAccountId(), "5.00", "4b10-transfer-contention-other");
            });
            assertThat(reversal.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isNotNull();
            assertThat(competingTransfer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isNotNull();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(accountRepository.findById(source.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("95.0000");
        assertThat(accountRepository.findById(destination.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("5.0000");
        assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)).hasSize(1);
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    @Test
    void replayUsesOriginalProjectionBalanceAfterAfterLaterActivity() {
        TestCustomer alice = customer("replay");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "100.00", "4b10-replay-original");
        ReversalReceipt first = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "first reason");
        depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "25.00", "4b10-replay-later");

        ReversalReceipt replay = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "different reason");
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.reversalJournalEntryId()).isEqualTo(first.reversalJournalEntryId());
        assertThat(replay.journalReference()).isEqualTo(first.journalReference());
        assertThat(replay.amount()).isEqualByComparingTo(first.amount());
        assertThat(replay.postedAt()).isEqualTo(first.postedAt());
        assertThat(replay.resultingCustomerBalances()).isEqualTo(first.resultingCustomerBalances());
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("25.0000");
        assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)).hasSize(1);
    }

    @Test
    void dependentSpendingRejectsDepositReversalAtomically() {
        TestCustomer alice = customer("dependent-deposit");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "100.00", "4b10-dependent-deposit");
        withdrawalWorkflow.withdraw(alice.user().getUserId(), account.getAccountId(), "80.00", "4b10-dependent-withdrawal");
        long journals = journalEntryRepository.count();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();

        assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "too late"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_INSUFFICIENT_FUNDS"));
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        assertThat(journalPostingRepository.count()).isEqualTo(postings);
        assertThat(transactionRepository.count()).isEqualTo(projections);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("20.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void dependentDestinationSpendingRejectsTransferReversalWithoutSourceCredit() {
        TestCustomer alice = customer("dependent-source");
        TestCustomer bob = customer("dependent-destination");
        Account source = account(alice, "100");
        Account destination = account(bob, "0");
        TransferReceipt original = transferWorkflow.transfer(alice.user().getUserId(), source.getAccountId(), destination.getAccountId(), "25.00", "4b10-dependent-transfer");
        withdrawalWorkflow.withdraw(bob.user().getUserId(), destination.getAccountId(), "20.00", "4b10-dependent-destination-withdrawal");
        long journals = journalEntryRepository.count();

        assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "destination spent funds"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_INSUFFICIENT_FUNDS"));
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        assertThat(accountRepository.findById(source.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("75.0000");
        assertThat(accountRepository.findById(destination.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("5.0000");
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    @Test
    void frozenClosedAndLedgerMismatchAccountsFailClosed() {
        TestCustomer frozenCustomer = customer("frozen");
        Account frozen = account(frozenCustomer, "0");
        DepositReceipt frozenOriginal = depositWorkflow.deposit(frozenCustomer.user().getUserId(), frozen.getAccountId(), "10.00", "4b10-frozen");
        frozen.setAccountStatus(AccountStatus.FROZEN);
        accountRepository.saveAndFlush(frozen);
        assertThatThrownBy(() -> reversalWorkflow.reverse(frozenCustomer.user().getUserId(), frozenOriginal.journalEntryId(), "frozen"))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_ACCOUNT_NOT_ACTIVE"));

        TestCustomer closedCustomer = customer("closed");
        Account closed = account(closedCustomer, "0");
        DepositReceipt closedOriginal = depositWorkflow.deposit(closedCustomer.user().getUserId(), closed.getAccountId(), "10.00", "4b10-closed");
        closed.setAccountStatus(AccountStatus.CLOSED);
        accountRepository.saveAndFlush(closed);
        assertThatThrownBy(() -> reversalWorkflow.reverse(closedCustomer.user().getUserId(), closedOriginal.journalEntryId(), "closed"))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_ACCOUNT_NOT_ACTIVE"));

        TestCustomer mismatchCustomer = customer("mismatch");
        Account mismatch = account(mismatchCustomer, "0");
        DepositReceipt mismatchOriginal = depositWorkflow.deposit(mismatchCustomer.user().getUserId(), mismatch.getAccountId(), "10.00", "4b10-mismatch");
        LedgerAccount customerLedger = ledgerAccountRepository.findByCustomerAccountId(mismatch.getAccountId()).orElseThrow();
        customerLedger.setStatus(LedgerAccountStatus.FROZEN);
        ledgerAccountRepository.saveAndFlush(customerLedger);
        assertThatThrownBy(() -> reversalWorkflow.reverse(mismatchCustomer.user().getUserId(), mismatchOriginal.journalEntryId(), "ledger mismatch"))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_LEDGER_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void systemAccountFailureAndForbiddenTargetsDoNotCreateRows() {
        TestCustomer alice = customer("system-failure");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "10.00", "4b10-system-failure");
        vault.setStatus(LedgerAccountStatus.FROZEN);
        ledgerAccountRepository.saveAndFlush(vault);
        long journals = journalEntryRepository.count();
        assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "vault unavailable"))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_LEDGER_ACCOUNT_NOT_ACTIVE"));
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        restoreVault();

        JournalEntry opening = new JournalEntry(UUID.randomUUID(), "TX-OPENING-" + UUID.randomUUID(),
                JournalEntryType.OPENING_BALANCE, JournalEntryStatus.POSTED, "LKR", new BigDecimal("1.0000"),
                "historical cutover", null, LedgerActorType.SYSTEM, null, "V3_MIGRATION", LedgerChannel.SYSTEM,
                LocalDateTime.now(ZoneOffset.UTC));
        journalEntryRepository.saveAndFlush(opening);
        assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), opening.getEntryId(), "cutover"))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_REVERSAL_NOT_ALLOWED"));

        ReversalReceipt first = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "valid");
        long afterFirst = journalEntryRepository.count();
        assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), first.reversalJournalEntryId(), "undo"))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_REVERSAL_NOT_ALLOWED"));
        assertThat(journalEntryRepository.count()).isEqualTo(afterFirst);
    }

    @Test
    void reasonValidationUsesFinalDescriptionLengthAndRejectsControls() {
        TestCustomer alice = customer("reason");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "10.00", "4b10-reason");
        String prefix = "Reversal of " + journalEntryRepository.findById(original.journalEntryId()).orElseThrow().getEntryReference() + ": ";
        String exact = "x".repeat(255 - prefix.length());
        ReversalReceipt accepted = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), exact);
        assertThat(journalEntryRepository.findById(accepted.reversalJournalEntryId()).orElseThrow().getDescription()).hasSize(255);

        TestCustomer second = customer("reason-over");
        Account secondAccount = account(second, "0");
        DepositReceipt secondOriginal = depositWorkflow.deposit(second.user().getUserId(), secondAccount.getAccountId(), "10.00", "4b10-reason-over");
        String secondPrefix = "Reversal of " + journalEntryRepository.findById(secondOriginal.journalEntryId()).orElseThrow().getEntryReference() + ": ";
        assertThatThrownBy(() -> reversalWorkflow.reverse(second.user().getUserId(), secondOriginal.journalEntryId(), "x".repeat(256 - secondPrefix.length())))
                .isInstanceOf(BusinessException.class).satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_REVERSAL_REASON_INVALID"));
        assertThatThrownBy(() -> reversalWorkflow.reverse(second.user().getUserId(), secondOriginal.journalEntryId(), "bad\nreason"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reversalWorkflow.reverse(second.user().getUserId(), secondOriginal.journalEntryId(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void postingFailureAndProjectionFailureRollBackAllReversalDml() throws Exception {
        TestCustomer alice = customer("rollback");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "10.00", "4b10-rollback");
        long journals = journalEntryRepository.count();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();

        createTrigger("trg_4b10_posting_fail", "BEFORE INSERT ON journal_postings", "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'test reversal posting failure'");
        try {
            assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "posting failure"))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            dropTrigger("trg_4b10_posting_fail");
        }
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        assertThat(journalPostingRepository.count()).isEqualTo(postings);
        assertThat(transactionRepository.count()).isEqualTo(projections);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("10.0000");

        createTrigger("trg_4b10_projection_fail", "BEFORE INSERT ON transactions", "IF NEW.description LIKE 'Reversal of %' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'test reversal projection failure'; END IF");
        try {
            assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "projection failure"))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            dropTrigger("trg_4b10_projection_fail");
        }
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        assertThat(journalPostingRepository.count()).isEqualTo(postings);
        assertThat(transactionRepository.count()).isEqualTo(projections);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("10.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void corruptOriginalPostingCurrencyAndAccountClassFailClosed() throws Exception {
        TestCustomer currencyCustomer = customer("corrupt-currency");
        Account currencyAccount = account(currencyCustomer, "0");
        DepositReceipt currencyOriginal = depositWorkflow.deposit(currencyCustomer.user().getUserId(), currencyAccount.getAccountId(), "10.00", "4b10-corrupt-currency");
        JournalPosting currencyPosting = journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(currencyOriginal.journalEntryId()).get(0);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE journal_postings SET currency = 'USD' WHERE posting_id = UNHEX(REPLACE('"
                    + currencyPosting.getPostingId() + "','-',''))");
        }
        assertThatThrownBy(() -> reversalWorkflow.reverse(currencyCustomer.user().getUserId(), currencyOriginal.journalEntryId(), "wrong currency"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_CORRUPT_ORIGINAL"));
        assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)).isEmpty();

        TestCustomer classCustomer = customer("corrupt-class");
        Account classAccount = account(classCustomer, "0");
        DepositReceipt classOriginal = depositWorkflow.deposit(classCustomer.user().getUserId(), classAccount.getAccountId(), "10.00", "4b10-corrupt-class");
        LedgerAccount customerLedger = ledgerAccountRepository.findByCustomerAccountId(classAccount.getAccountId()).orElseThrow();
        customerLedger.setAccountClass(LedgerAccountClass.ASSET);
        ledgerAccountRepository.saveAndFlush(customerLedger);
        assertThatThrownBy(() -> reversalWorkflow.reverse(classCustomer.user().getUserId(), classOriginal.journalEntryId(), "wrong class"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_CORRUPT_ORIGINAL"));
        assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)).isEmpty();
    }

    @Test
    void reversalRetryUsesFreshAttemptAndCommitsOneResult() {
        TestCustomer alice = customer("retry");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "10.00", "4b10-retry-original");
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            ReversalPostingResult result = (ReversalPostingResult) invocation.callRealMethod();
            if (calls.incrementAndGet() == 1) {
                throw new RuntimeException(new java.sql.SQLException("deadlock", "40001", 1213));
            }
            return result;
        }).when(postingEngineSpy).postReversal(any());

        ReversalReceipt receipt = reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "retry reversal");
        assertThat(calls).hasValue(2);
        assertThat(receipt.replayed()).isFalse();
        assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)).hasSize(1);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("0.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void reversalRetryExhaustionLeavesNoFinancialResidue() {
        TestCustomer alice = customer("retry-exhaustion");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "10.00", "4b10-retry-exhaustion-original");
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            attempts.incrementAndGet();
            throw new RuntimeException(new java.sql.SQLException("deadlock", "40001", 1213));
        }).when(postingEngineSpy).postReversal(any());
        long journals = journalEntryRepository.count();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();

        assertThatThrownBy(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "exhausted"))
                .isInstanceOf(com.bankingsystem.core.features.transactions.retry.FinancialTransactionRetryExhaustedException.class);
        assertThat(attempts).hasValue(3);
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        assertThat(journalPostingRepository.count()).isEqualTo(postings);
        assertThat(transactionRepository.count()).isEqualTo(projections);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("10.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void realMysqlDeadlockDuringReversalIsRetriedAsWholeTransaction() throws Exception {
        TestCustomer alice = customer("real-deadlock");
        Account account = account(alice, "0");
        DepositReceipt original = depositWorkflow.deposit(alice.user().getUserId(), account.getAccountId(), "10.00", "4b10-real-deadlock-original");
        String lockName = "4b10_reversal_gate_" + UUID.randomUUID().toString().replace("-", "");
        createDeadlockProbe(lockName);
        AtomicInteger reversalAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            reversalAttempts.incrementAndGet();
            return invocation.callRealMethod();
        }).when(postingEngineSpy).postReversal(any());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch competitorLocked = new CountDownLatch(1);
        Future<Void> competitor = executor.submit(() -> {
            new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.update("UPDATE retry_reversal_probe SET value = value + 1 WHERE id >= 3");
                jdbcTemplate.update("UPDATE retry_reversal_probe SET value = value + 1 WHERE id = 2");
                competitorLocked.countDown();
                while (Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT IS_FREE_LOCK(?)", Boolean.class, lockName))) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                }
                jdbcTemplate.update("UPDATE retry_reversal_probe SET value = value + 1 WHERE id = 1");
                return null;
            });
            return null;
        });
        Future<ReversalReceipt> target = null;
        try {
            assertThat(competitorLocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            target = executor.submit(() -> reversalWorkflow.reverse(alice.user().getUserId(), original.journalEntryId(), "real deadlock retry"));
            ReversalReceipt receipt = target.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            competitor.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(reversalAttempts).hasValue(2);
            assertThat(receipt.replayed()).isFalse();
            assertThat(journalEntryRepository.findAll().stream().filter(j -> j.getEntryType() == JournalEntryType.REVERSAL)).hasSize(1);
            assertThat(journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(receipt.reversalJournalEntryId())).hasSize(2);
            assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance()).isEqualByComparingTo("0.0000");
            assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
        } finally {
            if (target != null) {
                target.cancel(true);
            }
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            dropDeadlockProbe(lockName);
        }
    }

    private ReversalReceipt concurrentReverse(CyclicBarrier barrier, UUID actor, UUID original, String reason) throws Exception {
        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return reversalWorkflow.reverse(actor, original, reason);
    }

    private TestCustomer customer(String label) {
        Role role = roleRepository.findByRoleNameIgnoreCase("CUSTOMER").orElseGet(() -> {
            Role created = new Role();
            created.setRoleName("CUSTOMER");
            created.setDescription("Customer role for reversal tests");
            return roleRepository.saveAndFlush(created);
        });
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername(USER_PREFIX + label + "-" + suffix);
        user.setPasswordHash("$2a$10$reversal-test-hash");
        user.setEmail(USER_PREFIX + label + "-" + suffix + "@example.test");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user = userRepository.saveAndFlush(user);
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFirstName(label);
        customer.setLastName("Reversal");
        customer.setGender(Gender.OTHER);
        customer.setEmail(user.getEmail());
        customer.setPhone("+9477000" + suffix.substring(0, 5));
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setStatus(Status.ACTIVE);
        customer.setAddress("Reversal test address");
        customer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer = customerRepository.saveAndFlush(customer);
        return new TestCustomer(user, customer);
    }

    private Account account(TestCustomer owner, String initialDeposit) {
        Account account = new Account();
        account.setAccountNumber("4B10A-" + UUID.randomUUID().toString().replace("-", ""));
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("0.0000"));
        account.setCurrency("LKR");
        account.setCustomer(owner.customer());
        account.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account = accountRepository.saveAndFlush(account);
        ledgerAccountRepository.saveAndFlush(new LedgerAccount(UUID.randomUUID(), account.getAccountId(), null,
                LedgerAccountClass.LIABILITY, "LKR", LedgerAccountStatus.ACTIVE, LocalDateTime.now(ZoneOffset.UTC)));
        if (new BigDecimal(initialDeposit).signum() > 0) {
            seedDeposit(account, initialDeposit);
        }
        return account;
    }

    private void seedDeposit(Account account, String amountText) {
        LedgerAccount customerLedger = ledgerAccountRepository.findByCustomerAccountId(account.getAccountId()).orElseThrow();
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput(amountText, CurrencyCode.LKR);
        postingEngine.post(new PostingCommand(JournalEntryType.DEPOSIT, CurrencyCode.LKR, "Test seed",
                PostingActor.system("4B10A_TEST_SEED"), LedgerChannel.SYSTEM, List.of(
                new PostingInstruction(vault.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                new PostingInstruction(customerLedger.getLedgerAccountId(), PostingDirection.CREDIT, amount))));
    }

    private LedgerAccount ensureVault() {
        return ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR").orElseGet(() ->
                ledgerAccountRepository.saveAndFlush(new LedgerAccount(UUID.randomUUID(), null,
                        "SYSTEM_VAULT_CASH:LKR", LedgerAccountClass.ASSET, "LKR", LedgerAccountStatus.ACTIVE,
                        LocalDateTime.now(ZoneOffset.UTC))));
    }

    private void restoreVault() {
        ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR").ifPresent(value -> {
            value.setStatus(LedgerAccountStatus.ACTIVE);
            value.setAccountClass(LedgerAccountClass.ASSET);
            value.setCurrency("LKR");
            ledgerAccountRepository.saveAndFlush(value);
        });
    }

    private void createTrigger(String name, String timing, String body) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER IF EXISTS " + name);
            statement.execute("CREATE TRIGGER " + name + " " + timing + " FOR EACH ROW " + body);
        }
    }

    private void dropTrigger(String name) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER IF EXISTS " + name);
        }
    }

    private void createDeadlockProbe(String lockName) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER IF EXISTS trg_4b10_reversal_deadlock");
            statement.execute("DROP TABLE IF EXISTS retry_reversal_probe");
            statement.execute("CREATE TABLE retry_reversal_probe (id INT PRIMARY KEY, value INT NOT NULL) ENGINE=InnoDB");
            for (int id = 1; id <= 102; id++) {
                statement.execute("INSERT INTO retry_reversal_probe (id, value) VALUES (" + id + ", 0)");
            }
            statement.execute("CREATE TRIGGER trg_4b10_reversal_deadlock AFTER INSERT ON journal_entries FOR EACH ROW "
                    + "BEGIN IF NEW.entry_type = 'REVERSAL' THEN UPDATE retry_reversal_probe SET value = value + 1 WHERE id = 1; "
                    + "DO GET_LOCK('" + lockName + "', 10); DO SLEEP(1); UPDATE retry_reversal_probe SET value = value + 1 WHERE id = 2; "
                    + "DO RELEASE_LOCK('" + lockName + "'); END IF; END");
        }
    }

    private void dropDeadlockProbe(String lockName) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SELECT RELEASE_ALL_LOCKS()");
            statement.execute("DROP TRIGGER IF EXISTS trg_4b10_reversal_deadlock");
            statement.execute("DROP TABLE IF EXISTS retry_reversal_probe");
        }
    }

    private void cleanTestData() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.execute("DROP TRIGGER IF EXISTS trg_4b10_posting_fail");
            statement.execute("DROP TRIGGER IF EXISTS trg_4b10_projection_fail");
            statement.execute("DELETE FROM core_transaction_idempotency");
            statement.execute("DELETE FROM transactions");
            statement.execute("DELETE FROM journal_postings");
            statement.execute("DELETE FROM journal_entries");
            statement.execute("DELETE FROM ledger_accounts WHERE customer_account_id IS NOT NULL");
            statement.execute("DELETE FROM accounts");
            statement.execute("DELETE FROM customers WHERE email LIKE '4b10a-%'");
            statement.execute("DELETE FROM users WHERE email LIKE '4b10a-%'");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private record TestCustomer(User user, Customer customer) {
    }
}
