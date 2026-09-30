package com.bankingsystem.core.features.transactions.retry;

import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
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
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationService;
import com.bankingsystem.core.features.transactions.application.DepositWorkflow;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreOperationType;
import com.bankingsystem.core.features.transactions.idempotency.domain.repository.CoreTransactionIdempotencyRepository;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import com.bankingsystem.core.modules.common.enums.Gender;
import com.bankingsystem.core.modules.common.enums.Status;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

/** Proves a retry reloads mutable account state after the failed attempt rolls back. */
@SpringBootTest
class FinancialWorkflowStateRefreshIntegrationTest {

    @Autowired private DepositWorkflow depositWorkflow;
    @SpyBean private PostingEngine postingEngine;
    @MockBean private FinancialTransactionRetryBackoff backoff;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private LedgerAccountRepository ledgerAccountRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalPostingRepository journalPostingRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private CoreTransactionIdempotencyRepository idempotencyRepository;
    @Autowired private LedgerReconciliationService reconciliationService;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private LedgerAccount vault;

    @BeforeEach
    void setUp() throws Exception {
        cleanTestData();
        vault = ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR")
                .orElseGet(() -> ledgerAccountRepository.saveAndFlush(new LedgerAccount(
                        UUID.randomUUID(), null, "SYSTEM_VAULT_CASH:LKR", LedgerAccountClass.ASSET,
                        "LKR", LedgerAccountStatus.ACTIVE, LocalDateTime.now(ZoneOffset.UTC))));
    }

    @AfterEach
    void tearDown() throws Exception {
        reset(postingEngine);
        cleanTestData();
    }

    @Test
    void retrySeesAccountFrozenAfterFirstAttemptRollback() throws Exception {
        TestCustomer alice = createCustomer();
        Account account = createAccount(alice, "100.00");
        long journalsBefore = journalEntryRepository.count();
        long postingsBefore = journalPostingRepository.count();
        long transactionsBefore = transactionRepository.count();
        CountDownLatch firstPostingFinished = new CountDownLatch(1);
        CountDownLatch freezeCommitted = new CountDownLatch(1);
        AtomicInteger postCalls = new AtomicInteger();
        ExecutorService freezerPool = Executors.newSingleThreadExecutor();

        Future<?> freezer = freezerPool.submit(() -> {
            await(firstPostingFinished);
            new TransactionTemplate(transactionManager).execute(status -> {
                Account current = accountRepository.findById(account.getAccountId()).orElseThrow();
                current.setAccountStatus(AccountStatus.FROZEN);
                accountRepository.saveAndFlush(current);
                return null;
            });
            freezeCommitted.countDown();
            return null;
        });

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (postCalls.incrementAndGet() == 1) {
                firstPostingFinished.countDown();
                throw new RuntimeException(new SQLException("deadlock", "40001", 1213));
            }
            return result;
        }).when(postingEngine).post(any(PostingCommand.class));
        doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()).isFalse();
            await(freezeCommitted);
            return null;
        }).when(backoff).sleep(anyLong());

        try {
            assertThatThrownBy(() -> depositWorkflow.deposit(
                    alice.user().getUserId(), account.getAccountId(), "10.00", "state-refresh"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(failure -> assertThat(((BusinessException) failure).getCode())
                            .isEqualTo("ERR_ACCOUNT_NOT_ACTIVE"));
            freezer.get(10, TimeUnit.SECONDS);
        } finally {
            freezerPool.shutdownNow();
            assertThat(freezerPool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(postCalls).hasValue(1);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getAccountStatus())
                .isEqualTo(AccountStatus.FROZEN);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
        assertThat(journalEntryRepository.count()).isEqualTo(journalsBefore);
        assertThat(journalPostingRepository.count()).isEqualTo(postingsBefore);
        assertThat(transactionRepository.count()).isEqualTo(transactionsBefore);
        assertThat(idempotencyRepository.findByUserIdAndOperationTypeAndClientKey(
                alice.user().getUserId(), CoreOperationType.DEPOSIT, "state-refresh")).isEmpty();
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    private TestCustomer createCustomer() {
        Role role = roleRepository.findByRoleNameIgnoreCase("CUSTOMER").orElseGet(() -> {
            Role created = new Role();
            created.setRoleName("CUSTOMER");
            created.setDescription("Customer role for retry state test");
            return roleRepository.saveAndFlush(created);
        });
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername("4b8-state-" + suffix);
        user.setPasswordHash("$2a$10$workflow-test-hash");
        user.setEmail("4b8-state-" + suffix + "@example.test");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user = userRepository.saveAndFlush(user);
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFirstName("State");
        customer.setLastName("Refresh");
        customer.setGender(Gender.OTHER);
        customer.setEmail(user.getEmail());
        customer.setPhone("+9477000" + suffix.substring(0, 5));
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setStatus(Status.ACTIVE);
        customer.setAddress("Retry state test");
        customer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return new TestCustomer(user, customerRepository.saveAndFlush(customer));
    }

    private Account createAccount(TestCustomer owner, String amountText) {
        Account account = new Account();
        account.setAccountNumber("4B8-STATE-" + UUID.randomUUID().toString().replace("-", ""));
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("0.0000"));
        account.setCurrency("LKR");
        account.setCustomer(owner.customer());
        account.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        account = accountRepository.saveAndFlush(account);
        LedgerAccount customerLedger = ledgerAccountRepository.saveAndFlush(new LedgerAccount(
                UUID.randomUUID(), account.getAccountId(), null, LedgerAccountClass.LIABILITY,
                "LKR", LedgerAccountStatus.ACTIVE, LocalDateTime.now(ZoneOffset.UTC)));
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput(amountText, CurrencyCode.LKR);
        postingEngine.post(new PostingCommand(JournalEntryType.DEPOSIT, CurrencyCode.LKR, "State seed",
                PostingActor.system("4B8_STATE_SEED"), LedgerChannel.SYSTEM, List.of(
                new PostingInstruction(vault.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                new PostingInstruction(customerLedger.getLedgerAccountId(), PostingDirection.CREDIT, amount))));
        return account;
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
            statement.execute("DELETE FROM customers WHERE email LIKE '4b8-state-%'");
            statement.execute("DELETE FROM users WHERE email LIKE '4b8-state-%'");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("State refresh barrier timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("State refresh barrier interrupted", interrupted);
        }
    }

    private record TestCustomer(User user, Customer customer) {
    }
}
