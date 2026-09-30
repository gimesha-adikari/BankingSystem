package com.bankingsystem.core.features.transactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.application.*;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.*;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.features.transactions.idempotency.domain.repository.CoreTransactionIdempotencyRepository;
import com.bankingsystem.core.features.transactions.retry.FinancialTransactionRetryExhaustedException;
import com.bankingsystem.core.modules.common.enums.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/** Real HTTP proof for the ADMIN-only reversal transport boundary. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(ReversalApiIntegrationTest.ConcurrencyTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReversalApiIntegrationTest {

    private static final long TIMEOUT_SECONDS = 30;
    private static final String USER_PREFIX = "4b10b-api-";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DataSource dataSource;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private LedgerAccountRepository ledgerAccountRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalPostingRepository journalPostingRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private CoreTransactionIdempotencyRepository idempotencyRepository;
    @Autowired private PostingEngine postingEngine;
    @Autowired private DepositWorkflow depositWorkflow;
    @Autowired private WithdrawalWorkflow withdrawalWorkflow;
    @Autowired private TransferWorkflow transferWorkflow;
    @Autowired private ReversalWorkflow reversalWorkflow;
    @Autowired private LedgerReconciliationService reconciliationService;
    @Autowired private ConcurrencyGate concurrencyGate;
    @SpyBean private PostingEngine postingEngineSpy;

    private LedgerAccount vault;

    @BeforeEach
    void setUp() throws Exception {
        concurrencyGate.clear();
        cleanTestData();
        restoreVault();
        vault = ensureVault();
    }

    @AfterEach
    void tearDown() throws Exception {
        reset(postingEngineSpy);
        concurrencyGate.clear();
        restoreVault();
        cleanTestData();
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mvc.perform(post("/api/v1/transactions/" + UUID.randomUUID() + "/reversal")
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"unauthenticated\"}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "TELLER", "MANAGER", "EMPLOYEE"})
    void nonAdminRolesAreForbidden(String role) throws Exception {
        mvc.perform(post("/api/v1/transactions/" + UUID.randomUUID() + "/reversal")
                        .with(user("4b10b-role-" + role).roles(role))
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"not allowed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReverseDepositAndPersistsAuthenticatedActor() throws Exception {
        TestCustomer customer = customer("deposit");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "100.00", key("deposit"));
        JournalEntry originalBefore = journalEntryRepository.findById(original.journalEntryId()).orElseThrow();
        List<JournalPosting> originalPostings = journalPostingRepository
                .findByEntryIdOrderBySequenceNumberAsc(original.journalEntryId());
        long journalCount = journalEntryRepository.count();
        long postingCount = journalPostingRepository.count();
        long projectionCount = transactionRepository.count();
        long idempotencyCount = idempotencyRepository.count();
        User administrator = admin("deposit");

        Response response = reverse(administrator, original.journalEntryId(), "Correct duplicate deposit");

        assertThat(response.status()).isEqualTo(200);
        UUID reversalId = assertReceipt(response.body(), original.journalEntryId(), false);
        assertThat(journalEntryRepository.count()).isEqualTo(journalCount + 1);
        assertThat(journalPostingRepository.count()).isEqualTo(postingCount + 2);
        assertThat(transactionRepository.count()).isEqualTo(projectionCount + 1);
        assertThat(idempotencyRepository.count()).isEqualTo(idempotencyCount);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.0000");

        JournalEntry reversal = journalEntryRepository.findById(reversalId).orElseThrow();
        assertThat(reversal.getEntryType()).isEqualTo(JournalEntryType.REVERSAL);
        assertThat(reversal.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(reversal.getReversalOfEntryId()).isEqualTo(original.journalEntryId());
        assertThat(reversal.getActorType()).isEqualTo(LedgerActorType.USER);
        assertThat(reversal.getInitiatedByUserId()).isEqualTo(administrator.getUserId());
        assertThat(reversal.getSystemActorId()).isNull();
        assertThat(reversal.getChannel()).isEqualTo(LedgerChannel.WEB);
        assertThat(reversal.getDescription()).startsWith("Reversal of " + originalBefore.getEntryReference() + ":");
        assertInverse(originalPostings, reversalId);
        Transaction projection = transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversalId).get(0);
        assertThat(projection.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(projection.getBalanceAfter()).isEqualByComparingTo("0.0000");
        assertThat(projection.getCreatedAt()).isEqualTo(reversal.getPostedAt());
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void adminCanReverseWithdrawalAndProjectsDeposit() throws Exception {
        TestCustomer customer = customer("withdrawal");
        Account account = account(customer, "100");
        WithdrawalReceipt original = withdrawalWorkflow.withdraw(customer.user().getUserId(), account.getAccountId(),
                "40.00", key("withdrawal"));
        Response response = reverse(admin("withdrawal"), original.journalEntryId(), "Correct cash withdrawal");

        assertThat(response.status()).isEqualTo(200);
        UUID reversalId = assertReceipt(response.body(), original.journalEntryId(), false);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
        List<Transaction> projections = transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversalId);
        assertThat(projections).hasSize(1);
        assertThat(projections.get(0).getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(projections.get(0).getBalanceAfter()).isEqualByComparingTo("100.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void adminCanReverseTransferAndProjectsBothAccounts() throws Exception {
        TestCustomer sourceOwner = customer("transfer-source");
        TestCustomer destinationOwner = customer("transfer-destination");
        Account source = account(sourceOwner, "100");
        Account destination = account(destinationOwner, "20");
        TransferReceipt original = transferWorkflow.transfer(sourceOwner.user().getUserId(), source.getAccountId(),
                destination.getAccountId(), "25.00", key("transfer"));

        Response response = reverse(admin("transfer"), original.journalEntryId(), "Correct transfer");

        assertThat(response.status()).isEqualTo(200);
        UUID reversalId = assertReceipt(response.body(), original.journalEntryId(), false);
        assertThat(accountRepository.findById(source.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
        assertThat(accountRepository.findById(destination.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("20.0000");
        List<Transaction> projections = transactionRepository.findByJournalEntryIdOrderByCreatedAtAsc(reversalId);
        assertThat(projections).hasSize(2);
        assertThat(projections).extracting(Transaction::getType)
                .containsExactlyInAnyOrder(Transaction.TransactionType.TRANSFER_IN,
                        Transaction.TransactionType.TRANSFER_OUT);
        assertThat(projections).filteredOn(p -> p.getAccount().getAccountId().equals(source.getAccountId()))
                .singleElement().satisfies(p -> assertThat(p.getBalanceAfter()).isEqualByComparingTo("100.0000"));
        assertThat(projections).filteredOn(p -> p.getAccount().getAccountId().equals(destination.getAccountId()))
                .singleElement().satisfies(p -> assertThat(p.getBalanceAfter()).isEqualByComparingTo("20.0000"));
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    @Test
    void sequentialRepeatReturnsNaturalReplayWithoutNewRows() throws Exception {
        TestCustomer customer = customer("repeat");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "50.00", key("repeat-original"));
        User administrator = admin("repeat");

        Response first = reverse(administrator, original.journalEntryId(), "first reason");
        long journals = journalEntryRepository.count();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();
        Response second = reverse(administrator, original.journalEntryId(), "different reason ignored");

        assertThat(first.status()).isEqualTo(200);
        assertThat(second.status()).isEqualTo(200);
        UUID firstId = assertReceipt(first.body(), original.journalEntryId(), false);
        UUID secondId = assertReceipt(second.body(), original.journalEntryId(), true);
        assertThat(secondId).isEqualTo(firstId);
        assertThat(journalEntryRepository.count()).isEqualTo(journals);
        assertThat(journalPostingRepository.count()).isEqualTo(postings);
        assertThat(transactionRepository.count()).isEqualTo(projections);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.0000");
    }

    @Test
    void replayAfterLaterActivityReturnsOriginalReceiptBalances() throws Exception {
        TestCustomer customer = customer("stable-replay");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "100.00", key("stable-original"));
        User administrator = admin("stable-replay");

        Response first = reverse(administrator, original.journalEntryId(), "original correction");
        JsonNode firstBody = first.body();
        UUID reversalId = assertReceipt(firstBody, original.journalEntryId(), false);
        String originalPostedAt = firstBody.get("postedAt").asText();
        String originalBalance = firstBody.get("resultingCustomerBalances")
                .get(account.getAccountId().toString()).asText();

        depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(), "25.00", key("later-activity"));
        Response replay = reverse(administrator, original.journalEntryId(), "ignored later reason");

        assertThat(replay.status()).isEqualTo(200);
        assertThat(assertReceipt(replay.body(), original.journalEntryId(), true)).isEqualTo(reversalId);
        assertThat(replay.body().get("postedAt").asText()).isEqualTo(originalPostedAt);
        assertThat(replay.body().get("resultingCustomerBalances")
                .get(account.getAccountId().toString()).asText()).isEqualTo(originalBalance);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("25.0000");
        assertThat(journalEntryRepository.findAll().stream()
                .filter(j -> j.getEntryType() == JournalEntryType.REVERSAL).count()).isEqualTo(1);
    }

    @Test
    void forbiddenOpeningBalanceAndReversalTargetsReturnBusinessErrors() throws Exception {
        TestCustomer customer = customer("forbidden-target");
        Account account = account(customer, "0");
        User administrator = admin("forbidden-target");
        JournalEntry opening = new JournalEntry(UUID.randomUUID(), "TX-OPENING-" + UUID.randomUUID(),
                JournalEntryType.OPENING_BALANCE, JournalEntryStatus.POSTED, "LKR", new BigDecimal("1.0000"),
                "historical cutover", null, LedgerActorType.SYSTEM, null, "V3_MIGRATION", LedgerChannel.SYSTEM,
                LocalDateTime.now(ZoneOffset.UTC));
        journalEntryRepository.saveAndFlush(opening);

        Response openingResponse = reverse(administrator, opening.getEntryId(), "cutover correction");
        assertThat(openingResponse.status()).isEqualTo(422);
        assertThat(openingResponse.body().get("code").asText()).isEqualTo("ERR_REVERSAL_NOT_ALLOWED");

        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "10.00", key("forbidden-reversal-original"));
        ReversalReceipt existing = reversalWorkflow.reverse(administrator.getUserId(), original.journalEntryId(), "first");
        long reversalCount = journalEntryRepository.findAll().stream()
                .filter(j -> j.getEntryType() == JournalEntryType.REVERSAL).count();
        Response reversalResponse = reverse(administrator, existing.reversalJournalEntryId(), "undo");
        assertThat(reversalResponse.status()).isEqualTo(422);
        assertThat(reversalResponse.body().get("code").asText()).isEqualTo("ERR_REVERSAL_NOT_ALLOWED");
        assertThat(journalEntryRepository.findAll().stream()
                .filter(j -> j.getEntryType() == JournalEntryType.REVERSAL).count()).isEqualTo(reversalCount);
    }

    @Test
    void dependentSpendingFailsWithoutPartialReversal() throws Exception {
        TestCustomer customer = customer("dependent-spend");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "100.00", key("dependent-deposit"));
        withdrawalWorkflow.withdraw(customer.user().getUserId(), account.getAccountId(), "80.00", key("dependent-withdrawal"));
        User administrator = admin("dependent-spend");
        long reversals = countReversals();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();

        Response response = reverse(administrator, original.journalEntryId(), "cannot overdraw correction");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body().get("code").asText()).isEqualTo("ERR_INSUFFICIENT_FUNDS");
        assertThat(countReversals()).isEqualTo(reversals);
        assertThat(journalPostingRepository.count()).isEqualTo(postings);
        assertThat(transactionRepository.count()).isEqualTo(projections);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("20.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    @Test
    void transferDestinationSpendingFailsAtomically() throws Exception {
        TestCustomer sourceOwner = customer("spent-source");
        TestCustomer destinationOwner = customer("spent-destination");
        Account source = account(sourceOwner, "100");
        Account destination = account(destinationOwner, "0");
        TransferReceipt original = transferWorkflow.transfer(sourceOwner.user().getUserId(), source.getAccountId(),
                destination.getAccountId(), "25.00", key("spent-transfer"));
        withdrawalWorkflow.withdraw(destinationOwner.user().getUserId(), destination.getAccountId(), "20.00",
                key("spent-destination-withdrawal"));
        User administrator = admin("spent-transfer");
        long reversals = countReversals();

        Response response = reverse(administrator, original.journalEntryId(), "destination already spent funds");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body().get("code").asText()).isEqualTo("ERR_INSUFFICIENT_FUNDS");
        assertThat(countReversals()).isEqualTo(reversals);
        assertThat(accountRepository.findById(source.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("75.0000");
        assertThat(accountRepository.findById(destination.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("5.0000");
        assertThat(reconciliationService.reconcileAll().isClean()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FROZEN", "CLOSED"})
    void inactiveCustomerAccountFailsClosed(String statusName) throws Exception {
        TestCustomer customer = customer("status-" + statusName.toLowerCase(Locale.ROOT));
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "10.00", key("status-original-" + statusName));
        Account statusAccount = accountRepository.findById(account.getAccountId()).orElseThrow();
        statusAccount.setAccountStatus(AccountStatus.valueOf(statusName));
        accountRepository.saveAndFlush(statusAccount);
        long reversals = countReversals();

        Response response = reverse(admin("status-" + statusName), original.journalEntryId(), "status correction");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body().get("code").asText()).isEqualTo("ERR_ACCOUNT_NOT_ACTIVE");
        assertThat(countReversals()).isEqualTo(reversals);
        LedgerReconciliationResult reconciliation = reconciliationService.reconcileAccount(account.getAccountId());
        assertThat(reconciliation.isClean())
                .as("reconciliation after %s failure: %s", statusName, reconciliation.getDiscrepancies())
                .isTrue();
    }

    @Test
    void ledgerAccountStatusMismatchFailsClosed() throws Exception {
        TestCustomer customer = customer("status-mismatch");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "10.00", key("status-mismatch-original"));
        LedgerAccount ledger = ledgerAccountRepository.findByCustomerAccountId(account.getAccountId()).orElseThrow();
        ledger.setStatus(LedgerAccountStatus.FROZEN);
        ledgerAccountRepository.saveAndFlush(ledger);

        Response response = reverse(admin("status-mismatch"), original.journalEntryId(), "status mismatch");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body().get("code").asText()).isEqualTo("ERR_LEDGER_ACCOUNT_NOT_ACTIVE");
        assertThat(countReversals()).isZero();
    }

    @Test
    void systemAccountFailureFailsWithoutSubstitution() throws Exception {
        TestCustomer customer = customer("system-failure");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "10.00", key("system-failure-original"));
        vault.setStatus(LedgerAccountStatus.FROZEN);
        ledgerAccountRepository.saveAndFlush(vault);

        Response response = reverse(admin("system-failure"), original.journalEntryId(), "vault unavailable");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body().get("code").asText()).isEqualTo("ERR_LEDGER_ACCOUNT_NOT_ACTIVE");
        assertThat(countReversals()).isZero();
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10.0000");
    }

    @Test
    void invalidReasonInputsAreRejectedBeforeAReversal() throws Exception {
        TestCustomer customer = customer("invalid-reason");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "10.00", key("invalid-reason-original"));
        User administrator = admin("invalid-reason");
        String path = reversalPath(original.journalEntryId());

        assertThat(mvc.perform(post(path).with(adminPrincipal(administrator)).contentType(APPLICATION_JSON))
                .andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(mvc.perform(post(path).with(adminPrincipal(administrator)).contentType(APPLICATION_JSON)
                .content("{}")).andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(mvc.perform(post(path).with(adminPrincipal(administrator)).contentType(APPLICATION_JSON)
                .content("{\"reason\":\"   \"}")).andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(mvc.perform(post(path).with(adminPrincipal(administrator)).contentType(APPLICATION_JSON)
                .content("{\"reason\":\"bad\\nreason\"}")).andReturn().getResponse().getStatus()).isEqualTo(400);

        String prefix = "Reversal of " + journalEntryRepository.findById(original.journalEntryId()).orElseThrow()
                .getEntryReference() + ": ";
        String overLimit = "x".repeat(256 - prefix.length());
        MvcResult overLimitResult = mvc.perform(post(path).with(adminPrincipal(administrator))
                        .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("reason", overLimit))))
                .andReturn();
        assertThat(overLimitResult.getResponse().getStatus()).isEqualTo(422);
        assertThat(objectMapper.readTree(overLimitResult.getResponse().getContentAsString()).get("code").asText())
                .isEqualTo("ERR_REVERSAL_REASON_INVALID");
        assertThat(countReversals()).isZero();
    }

    @Test
    void invalidUuidAndUnknownOriginalAreControlledErrors() throws Exception {
        User administrator = admin("invalid-path");
        mvc.perform(post("/api/v1/transactions/not-a-uuid/reversal").with(adminPrincipal(administrator))
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"invalid path\"}"))
                .andExpect(status().isBadRequest());

        MvcResult missing = mvc.perform(post(reversalPath(UUID.randomUUID())).with(adminPrincipal(administrator))
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"missing original\"}"))
                .andReturn();
        assertThat(missing.getResponse().getStatus()).isEqualTo(404);
        assertThat(objectMapper.readTree(missing.getResponse().getContentAsString()).get("code").asText())
                .isEqualTo("ERR_REVERSAL_ORIGINAL_NOT_FOUND");
    }

    @Test
    void retryExhaustionUsesSanitized503Mapping() throws Exception {
        TestCustomer customer = customer("retry-exhaustion");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "10.00", key("retry-exhaustion-original"));
        User administrator = admin("retry-exhaustion");
        doThrow(new FinancialTransactionRetryExhaustedException("REVERSAL", 3,
                new RuntimeException("SQLState 40001 / MySQL 1213")))
                .when(postingEngineSpy).postReversal(any());

        Response response = reverse(administrator, original.journalEntryId(), "retry exhaustion");

        assertThat(response.status()).isEqualTo(503);
        assertThat(response.body().get("code").asText()).isEqualTo("ERR_TRANSACTION_TEMPORARILY_UNAVAILABLE");
        assertThat(response.body().get("message").asText()).isEqualTo("The transaction is temporarily unavailable");
        assertThat(countReversals()).isZero();
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10.0000");
    }

    @Test
    void sameOriginalConcurrentAdminRequestsCommitExactlyOneReversal() throws Exception {
        TestCustomer customer = customer("concurrent");
        Account account = account(customer, "0");
        DepositReceipt original = depositWorkflow.deposit(customer.user().getUserId(), account.getAccountId(),
                "100.00", key("concurrent-original"));
        User administrator = admin("concurrent");
        String path = reversalPath(original.journalEntryId());
        long journals = journalEntryRepository.count();
        long postings = journalPostingRepository.count();
        long projections = transactionRepository.count();
        long idempotencyRows = idempotencyRepository.count();
        concurrencyGate.arm(path);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Response>> futures;
        try {
            futures = List.of(
                    executor.submit(() -> reverse(administrator, original.journalEntryId(), "first concurrent")),
                    executor.submit(() -> reverse(administrator, original.journalEntryId(), "second concurrent")));
            Response first = futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Response second = futures.get(1).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(first.status()).isEqualTo(200);
            assertThat(second.status()).isEqualTo(200);
            UUID firstId = assertReceipt(first.body(), original.journalEntryId(), first.body().get("replayed").asBoolean());
            UUID secondId = assertReceipt(second.body(), original.journalEntryId(), second.body().get("replayed").asBoolean());
            assertThat(firstId).isEqualTo(secondId);
            assertThat(List.of(first.body().get("replayed").asBoolean(), second.body().get("replayed").asBoolean()))
                    .containsExactlyInAnyOrder(false, true);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(concurrencyGate.arrivals()).isEqualTo(2);
        assertThat(concurrencyGate.released()).isTrue();
        assertThat(journalEntryRepository.count()).isEqualTo(journals + 1);
        assertThat(journalPostingRepository.count()).isEqualTo(postings + 2);
        assertThat(transactionRepository.count()).isEqualTo(projections + 1);
        assertThat(idempotencyRepository.count()).isEqualTo(idempotencyRows);
        assertThat(accountRepository.findById(account.getAccountId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.0000");
        assertThat(reconciliationService.reconcileAccount(account.getAccountId()).isClean()).isTrue();
    }

    private Response reverse(User administrator, UUID originalId, String reason) throws Exception {
        MvcResult result = mvc.perform(post(reversalPath(originalId)).with(adminPrincipal(administrator))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", reason))))
                .andReturn();
        return new Response(result.getResponse().getStatus(), objectMapper.readTree(result.getResponse().getContentAsString()));
    }

    private UUID assertReceipt(JsonNode body, UUID originalId, boolean replayed) {
        assertThat(body.get("operationType").asText()).isEqualTo("REVERSAL");
        assertThat(UUID.fromString(body.get("reversalOfEntryId").asText())).isEqualTo(originalId);
        assertThat(new BigDecimal(body.get("amount").asText())).isPositive();
        assertThat(body.get("currency").asText()).isEqualTo("LKR");
        assertThat(body.get("replayed").asBoolean()).isEqualTo(replayed);
        assertThat(body.get("resultingCustomerBalances").isObject()).isTrue();
        return UUID.fromString(body.get("reversalJournalEntryId").asText());
    }

    private void assertInverse(List<JournalPosting> original, UUID reversalId) {
        List<JournalPosting> inverse = journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(reversalId);
        assertThat(inverse).hasSize(original.size());
        for (int i = 0; i < original.size(); i++) {
            assertThat(inverse.get(i).getSequenceNumber()).isEqualTo(i);
            assertThat(inverse.get(i).getLedgerAccountId()).isEqualTo(original.get(i).getLedgerAccountId());
            assertThat(inverse.get(i).getAmount()).isEqualByComparingTo(original.get(i).getAmount());
            assertThat(inverse.get(i).getDirection()).isNotEqualTo(original.get(i).getDirection());
            assertThat(inverse.get(i).getCurrency()).isEqualTo(original.get(i).getCurrency());
        }
    }

    private RequestPostProcessor adminPrincipal(User administrator) {
        return SecurityMockMvcRequestPostProcessors.user(administrator.getUsername()).roles("ADMIN");
    }

    private String reversalPath(UUID originalId) {
        return "/api/v1/transactions/" + originalId + "/reversal";
    }

    private String key(String label) {
        return USER_PREFIX + label + "-" + UUID.randomUUID();
    }

    private long countReversals() {
        return journalEntryRepository.findAll().stream()
                .filter(j -> j.getEntryType() == JournalEntryType.REVERSAL).count();
    }

    private TestCustomer customer(String label) {
        Role role = role("CUSTOMER");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername(USER_PREFIX + label + "-" + suffix);
        user.setPasswordHash("$2a$10$reversal-api-test-hash");
        user.setEmail(USER_PREFIX + label + "-" + suffix + "@example.test");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user = userRepository.saveAndFlush(user);
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFirstName(label);
        customer.setLastName("ReversalApi");
        customer.setGender(Gender.OTHER);
        customer.setEmail(user.getEmail());
        customer.setPhone("+9477000" + suffix.substring(0, 5));
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setStatus(Status.ACTIVE);
        customer.setAddress("Reversal API test address");
        customer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        customer = customerRepository.saveAndFlush(customer);
        return new TestCustomer(user, customer);
    }

    private User admin(String label) {
        Role role = role("ADMIN");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername(USER_PREFIX + "admin-" + label + "-" + suffix);
        user.setPasswordHash("$2a$10$reversal-api-test-hash");
        user.setEmail(USER_PREFIX + "admin-" + label + "-" + suffix + "@example.test");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return userRepository.saveAndFlush(user);
    }

    private Role role(String name) {
        return roleRepository.findByRoleNameIgnoreCase(name).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(name);
            role.setDescription("Reversal API test role");
            return roleRepository.saveAndFlush(role);
        });
    }

    private Account account(TestCustomer owner, String initialDeposit) {
        Account account = new Account();
        account.setAccountNumber("4B10B-" + UUID.randomUUID().toString().replace("-", ""));
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
                PostingActor.system("4B10B_API_SEED"), LedgerChannel.SYSTEM, List.of(
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

    private void cleanTestData() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.execute("DELETE FROM core_transaction_idempotency");
            statement.execute("DELETE FROM transactions");
            statement.execute("DELETE FROM journal_postings");
            statement.execute("DELETE FROM journal_entries");
            statement.execute("DELETE FROM ledger_accounts WHERE customer_account_id IS NOT NULL");
            statement.execute("DELETE FROM accounts");
            statement.execute("DELETE FROM customers WHERE email LIKE '" + USER_PREFIX + "%'");
            statement.execute("DELETE FROM users WHERE email LIKE '" + USER_PREFIX + "%'");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private record TestCustomer(User user, Customer customer) {
    }

    private record Response(int status, JsonNode body) {
    }

    @TestConfiguration
    static class ConcurrencyTestConfiguration {
        @Bean
        ConcurrencyGate concurrencyGate() {
            return new ConcurrencyGate();
        }

        @Bean
        FilterRegistrationBean<Filter> reversalHttpBarrier(ConcurrencyGate gate) {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
            registration.setFilter((ServletRequest request, ServletResponse response, FilterChain chain) -> {
                if (request instanceof HttpServletRequest httpRequest) {
                    gate.awaitIfArmed(httpRequest.getRequestURI());
                }
                chain.doFilter(request, response);
            });
            registration.addUrlPatterns("/api/v1/transactions/*");
            registration.setOrder(Integer.MIN_VALUE);
            return registration;
        }
    }

    static final class ConcurrencyGate {
        private volatile String armedPath;
        private volatile CyclicBarrier barrier;
        private final AtomicInteger arrivals = new AtomicInteger();
        private final AtomicBoolean released = new AtomicBoolean();

        void arm(String path) {
            armedPath = path;
            arrivals.set(0);
            released.set(false);
            barrier = new CyclicBarrier(2, () -> released.set(true));
        }

        void clear() {
            armedPath = null;
            barrier = null;
        }

        void awaitIfArmed(String requestPath) throws ServletException {
            if (!Objects.equals(armedPath, requestPath)) {
                return;
            }
            arrivals.incrementAndGet();
            try {
                barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ServletException("Reversal HTTP barrier interrupted", ex);
            } catch (BrokenBarrierException | TimeoutException ex) {
                throw new ServletException("Reversal HTTP barrier did not release", ex);
            }
        }

        int arrivals() {
            return arrivals.get();
        }

        boolean released() {
            return released.get();
        }
    }
}
