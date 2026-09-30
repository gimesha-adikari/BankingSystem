package com.bankingsystem.core.features.accounts;

import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.accounts.application.AccountService;
import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.accounts.interfaces.dto.AccountRequestDTO;
import com.bankingsystem.core.features.accounts.interfaces.dto.AccountResponseDTO;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.branch.domain.Branch;
import com.bankingsystem.core.features.branch.domain.repository.BranchRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationResult;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationService;
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
import com.bankingsystem.core.modules.common.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class AccountOpeningIntegrationTest {

    @Autowired
    private AccountService accountService;

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
    private LedgerReconciliationService reconciliationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DataSource dataSource;

    private User customerUser;
    private Customer customer;
    private Branch branch;
    private LedgerAccount vaultCashAccount;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        // 1. Fetch or create Customer role & user
        Role customerRole = roleRepository.findByRoleNameIgnoreCase("CUSTOMER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName("CUSTOMER");
                    r.setDescription("Customer Role");
                    return roleRepository.save(r);
                });

        customerUser = userRepository.findByUsername("customer")
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("customer");
                    u.setPasswordHash("$2a$10$dummyHashForTestingAccountOpening");
                    u.setRole(customerRole);
                    u.setEmail("customer_acct_open@example.test");
                    u.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    return userRepository.save(u);
                });

        // 2. Fetch or create test customer
        customer = customerRepository.findByUserUserId(customerUser.getUserId())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setUser(customerUser);
                    c.setFirstName("Alice");
                    c.setLastName("Opener");
                    c.setEmail("customer_acct_open@example.test");
                    c.setPhone("+94770001122");
                    c.setGender(com.bankingsystem.core.modules.common.enums.Gender.MALE);
                    c.setStatus(com.bankingsystem.core.modules.common.enums.Status.ACTIVE);
                    c.setDateOfBirth(LocalDate.of(2000, 1, 1));
                    c.setAddress("123 Bank St, Colombo");
                    c.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    c.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    return customerRepository.save(c);
                });

        // 3. Ensure test branch exists
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setBranchName("Colombo Central");
            b.setAddress("Colombo 01");
            return branchRepository.save(b);
        });

        // 4. Ensure system vault cash ledger account exists
        vaultCashAccount = ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR")
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

        // Set security context for authenticated customer
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        customerUser.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                )
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    private void cleanDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_ledger_accounts");
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_journal_postings");
            stmt.execute("DROP TRIGGER IF EXISTS trg_test_fail_opening_projection");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM journal_postings");
            stmt.execute("DELETE FROM journal_entries");
            stmt.execute("DELETE FROM ledger_accounts WHERE customer_account_id IS NOT NULL");
            stmt.execute("DELETE FROM accounts");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    // =========================================================================
    // STEP 21: Positive Account-Opening Integration Test
    // =========================================================================
    @Test
    void testPositiveOpeningDepositPostsThroughPostingEngineAtomically() {
        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("1500.00"));
        req.setBranchId(branch.getBranchId());

        AccountResponseDTO res = accountService.openAccount(req, null);

        assertThat(res).isNotNull();
        assertThat(res.getAccountId()).isNotNull();
        assertThat(res.getAccountNumber()).hasSize(10).matches("^[0-9]{10}$");
        assertThat(res.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(res.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(res.getBalance()).isEqualByComparingTo("1500.0000");

        // 1. Account verified
        Account acc = accountRepository.findById(res.getAccountId()).orElseThrow();
        assertThat(acc.getBalance()).isEqualByComparingTo("1500.0000");
        assertThat(acc.getCurrency()).isEqualTo("LKR");
        assertThat(acc.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());

        // 2. Customer liability LedgerAccount verified
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        assertThat(la.getAccountClass()).isEqualTo(LedgerAccountClass.LIABILITY);
        assertThat(la.getCurrency()).isEqualTo("LKR");
        assertThat(la.getStatus()).isEqualTo(LedgerAccountStatus.ACTIVE);
        assertThat(la.getSystemCode()).isNull();

        // 3. Journal Entry verified
        List<JournalEntry> journals = journalEntryRepository.findAll();
        assertThat(journals).hasSize(1);
        JournalEntry entry = journals.get(0);
        assertThat(entry.getEntryType()).isEqualTo(JournalEntryType.DEPOSIT);
        assertThat(entry.getTotalAmount()).isEqualByComparingTo("1500.0000");
        assertThat(entry.getActorType()).isEqualTo(LedgerActorType.USER);
        assertThat(entry.getInitiatedByUserId()).isEqualTo(customerUser.getUserId());
        assertThat(entry.getChannel()).isEqualTo(LedgerChannel.WEB);
        assertThat(entry.getDescription()).isEqualTo("Opening deposit");

        // 4. Journal Postings verified
        List<JournalPosting> postings = journalPostingRepository.findByEntryIdOrderBySequenceNumberAsc(entry.getEntryId());
        assertThat(postings).hasSize(2);
        JournalPosting vaultDebit = postings.get(0);
        assertThat(vaultDebit.getLedgerAccountId()).isEqualTo(vaultCashAccount.getLedgerAccountId());
        assertThat(vaultDebit.getDirection()).isEqualTo(PostingDirection.DEBIT);
        assertThat(vaultDebit.getAmount()).isEqualByComparingTo("1500.0000");

        JournalPosting customerCredit = postings.get(1);
        assertThat(customerCredit.getLedgerAccountId()).isEqualTo(la.getLedgerAccountId());
        assertThat(customerCredit.getDirection()).isEqualTo(PostingDirection.CREDIT);
        assertThat(customerCredit.getAmount()).isEqualByComparingTo("1500.0000");

        // 5. Legacy Transaction projection verified
        List<Transaction> txList = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(acc.getAccountId());
        assertThat(txList).hasSize(1);
        Transaction tx = txList.get(0);
        assertThat(tx.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualByComparingTo("1500.0000");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("1500.0000");
        assertThat(tx.getDescription()).isEqualTo("Opening deposit");
        assertThat(tx.getJournalEntryId()).isEqualTo(entry.getEntryId());

        // 6. Reconciliation clean
        LedgerReconciliationResult recResult = reconciliationService.reconcileAccount(acc.getAccountId());
        assertThat(recResult.isClean()).isTrue();
    }

    // =========================================================================
    // STEP 22: Zero-Deposit Integration Test (Checking Account)
    // =========================================================================
    @Test
    void testZeroDepositAccountOpeningCreatesAccountAndLedgerWithoutJournal() {
        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.CHECKING);
        req.setInitialDeposit(BigDecimal.ZERO);
        req.setBranchId(branch.getBranchId());

        AccountResponseDTO res = accountService.openAccount(req, null);

        assertThat(res).isNotNull();
        assertThat(res.getBalance()).isEqualByComparingTo("0.0000");

        // Account exists with zero balance
        Account acc = accountRepository.findById(res.getAccountId()).orElseThrow();
        assertThat(acc.getBalance()).isEqualByComparingTo("0.0000");
        assertThat(acc.getAccountType()).isEqualTo(AccountType.CHECKING);

        // LedgerAccount exists
        LedgerAccount la = ledgerAccountRepository.findByCustomerAccountId(acc.getAccountId()).orElseThrow();
        assertThat(la.getAccountClass()).isEqualTo(LedgerAccountClass.LIABILITY);

        // NO journal entry, NO postings, NO legacy transactions created
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(journalPostingRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();

        // Reconciliation clean (0 == 0)
        LedgerReconciliationResult recResult = reconciliationService.reconcileAccount(acc.getAccountId());
        assertThat(recResult.isClean()).isTrue();
    }

    // =========================================================================
    // STEP 23: Fractional-Cent Customer Input Rejection
    // =========================================================================
    @Test
    void testFractionalCentCustomerInputIsRejectedWithoutPersistence() {
        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("1500.001"));
        req.setBranchId(branch.getBranchId());

        assertThatThrownBy(() -> accountService.openAccount(req, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_DEPOSIT_INVALID"));

        // Verify zero persistence
        assertThat(accountRepository.count()).isZero();
        assertThat(ledgerAccountRepository.findAll().stream().filter(LedgerAccount::isCustomerAccount)).isEmpty();
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(journalPostingRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }

    // =========================================================================
    // STEP 24: Negative Initial Deposit Rejection
    // =========================================================================
    @Test
    void testNegativeDepositIsRejectedWithoutPersistence() {
        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("-100.00"));
        req.setBranchId(branch.getBranchId());

        assertThatThrownBy(() -> accountService.openAccount(req, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_DEPOSIT_INVALID"));

        assertThat(accountRepository.count()).isZero();
    }

    // =========================================================================
    // STEP 25: Missing Vault Account Failure Rollback
    // =========================================================================
    @Test
    void testMissingVaultRollsBackAccountOpeningEntirely() {
        // Temporarily mutate system vault code
        vaultCashAccount.setSystemCode("SYSTEM_VAULT_CASH:DISABLED");
        ledgerAccountRepository.saveAndFlush(vaultCashAccount);

        try {
            AccountRequestDTO req = new AccountRequestDTO();
            req.setAccountType(AccountType.SAVINGS);
            req.setInitialDeposit(new BigDecimal("1500.00"));
            req.setBranchId(branch.getBranchId());

            assertThatThrownBy(() -> accountService.openAccount(req, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_VAULT_ACCOUNT_NOT_FOUND"));

            // Verify entire transaction rolled back: 0 accounts, 0 customer ledger accounts
            assertThat(accountRepository.count()).isZero();
            assertThat(ledgerAccountRepository.findAll().stream().filter(LedgerAccount::isCustomerAccount)).isEmpty();
            assertThat(journalEntryRepository.count()).isZero();
            assertThat(journalPostingRepository.count()).isZero();
            assertThat(transactionRepository.count()).isZero();
        } finally {
            vaultCashAccount.setSystemCode("SYSTEM_VAULT_CASH:LKR");
            ledgerAccountRepository.saveAndFlush(vaultCashAccount);
        }
    }

    // =========================================================================
    // STEP 26: Customer Ledger Creation Failure Rollback
    // =========================================================================
    @Test
    void testCustomerLedgerCreationFailureRollsBackAccount() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_ledger_accounts BEFORE INSERT ON ledger_accounts " +
                    "FOR EACH ROW BEGIN " +
                    "  IF NEW.customer_account_id IS NOT NULL THEN " +
                    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Simulated ledger account creation failure'; " +
                    "  END IF; " +
                    "END;");
        }

        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("1500.00"));
        req.setBranchId(branch.getBranchId());

        assertThatThrownBy(() -> accountService.openAccount(req, null))
                .isNotNull();

        // Verify account creation was rolled back by transaction
        assertThat(accountRepository.count()).isZero();
        assertThat(ledgerAccountRepository.findAll().stream().filter(LedgerAccount::isCustomerAccount)).isEmpty();
    }

    // =========================================================================
    // STEP 27: Journal / Posting Failure Rollback
    // =========================================================================
    @Test
    void testPostingFailureRollsBackAccountAndCustomerLedger() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_journal_postings BEFORE INSERT ON journal_postings " +
                    "FOR EACH ROW BEGIN " +
                    "  IF NEW.amount = 1555.0000 THEN " +
                    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Simulated posting failure during opening'; " +
                    "  END IF; " +
                    "END;");
        }

        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("1555.00"));
        req.setBranchId(branch.getBranchId());

        assertThatThrownBy(() -> accountService.openAccount(req, null))
                .isNotNull();

        // Verify rollback: Account gone, customer LedgerAccount gone, journal gone
        assertThat(accountRepository.count()).isZero();
        assertThat(ledgerAccountRepository.findAll().stream().filter(LedgerAccount::isCustomerAccount)).isEmpty();
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(journalPostingRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }

    // =========================================================================
    // STEP 28: Legacy Projection Failure Rollback
    // =========================================================================
    @Test
    void testLegacyProjectionFailureRollsBackEntireAccountOpening() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TRIGGER trg_test_fail_opening_projection BEFORE INSERT ON transactions " +
                    "FOR EACH ROW BEGIN " +
                    "  IF NEW.description = 'Opening deposit' AND NEW.amount = 1666.0000 THEN " +
                    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Simulated projection insert failure'; " +
                    "  END IF; " +
                    "END;");
        }

        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("1666.00"));
        req.setBranchId(branch.getBranchId());

        assertThatThrownBy(() -> accountService.openAccount(req, null))
                .isNotNull();

        // Verify total rollback
        assertThat(accountRepository.count()).isZero();
        assertThat(ledgerAccountRepository.findAll().stream().filter(LedgerAccount::isCustomerAccount)).isEmpty();
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(journalPostingRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }

    // =========================================================================
    // STEP 29: Existing Validation & Prerequisites
    // =========================================================================
    @Test
    void testExistingValidationPrerequisitesEnforced() {
        // 1. Below minimum deposit for SAVINGS (min is 1000.00)
        AccountRequestDTO lowDepositReq = new AccountRequestDTO();
        lowDepositReq.setAccountType(AccountType.SAVINGS);
        lowDepositReq.setInitialDeposit(new BigDecimal("500.00"));
        lowDepositReq.setBranchId(branch.getBranchId());

        assertThatThrownBy(() -> accountService.openAccount(lowDepositReq, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERR_MIN_DEPOSIT"));

        // 2. Missing branch
        AccountRequestDTO missingBranchReq = new AccountRequestDTO();
        missingBranchReq.setAccountType(AccountType.SAVINGS);
        missingBranchReq.setInitialDeposit(new BigDecimal("1500.00"));
        missingBranchReq.setBranchId(999999);

        assertThatThrownBy(() -> accountService.openAccount(missingBranchReq, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Branch not found");
    }

    // =========================================================================
    // STEP 30 & 31: V1 History Compatibility & Security Ownership
    // =========================================================================
    @Test
    void testHistoryCompatibilityReturnsDirectArrayWithSingleOpeningDeposit() {
        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("2000.00"));
        req.setBranchId(branch.getBranchId());

        AccountResponseDTO res = accountService.openAccount(req, null);

        // Fetch account transactions via transaction query service
        List<TransactionResponseDTO> history = transactionQueryService.getTransactionsForAccount(res.getAccountId());
        assertThat(history).hasSize(1);

        TransactionResponseDTO dto = history.get(0);
        assertThat(dto.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(dto.getAmount()).isEqualByComparingTo("2000.0000");
        assertThat(dto.getBalanceAfter()).isEqualByComparingTo("2000.0000");
        assertThat(dto.getDescription()).isEqualTo("Opening deposit");
    }

    // =========================================================================
    // STEP 32: Historical Accounts Must Remain Unchanged
    // =========================================================================
    @Test
    void testHistoricalAccountsRemainUntouchedDuringNewAccountOpening() {
        // 1. Create a pre-existing historical account
        Account historicalAcc = new Account();
        historicalAcc.setAccountNumber("1111222233");
        historicalAcc.setAccountType(AccountType.SAVINGS);
        historicalAcc.setAccountStatus(AccountStatus.ACTIVE);
        historicalAcc.setCurrency("LKR");
        historicalAcc.setBalance(new BigDecimal("500.0000"));
        historicalAcc.setCustomer(customer);
        historicalAcc.setBranch(branch);
        historicalAcc.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10));
        historicalAcc.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10));
        historicalAcc = accountRepository.saveAndFlush(historicalAcc);

        LedgerAccount historicalLa = new LedgerAccount(
                UUID.randomUUID(),
                historicalAcc.getAccountId(),
                null,
                LedgerAccountClass.LIABILITY,
                "LKR",
                LedgerAccountStatus.ACTIVE,
                LocalDateTime.now(ZoneOffset.UTC).minusDays(10)
        );
        ledgerAccountRepository.saveAndFlush(historicalLa);

        Transaction historicalTx = new Transaction();
        historicalTx.setAccount(historicalAcc);
        historicalTx.setType(Transaction.TransactionType.DEPOSIT);
        historicalTx.setAmount(new BigDecimal("500.0000"));
        historicalTx.setBalanceAfter(new BigDecimal("500.0000"));
        historicalTx.setDescription("Historical opening balance");
        historicalTx.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10));
        historicalTx.setJournalEntryId(null); // Pre-ledger historical row
        transactionRepository.saveAndFlush(historicalTx);

        // 2. Open a new account through AccountService
        AccountRequestDTO req = new AccountRequestDTO();
        req.setAccountType(AccountType.SAVINGS);
        req.setInitialDeposit(new BigDecimal("1500.00"));
        req.setBranchId(branch.getBranchId());
        AccountResponseDTO newAccRes = accountService.openAccount(req, null);

        // 3. Verify historical account is completely untouched
        Account reloadedHistorical = accountRepository.findById(historicalAcc.getAccountId()).orElseThrow();
        assertThat(reloadedHistorical.getBalance()).isEqualByComparingTo("500.0000");
        assertThat(reloadedHistorical.getAccountNumber()).isEqualTo("1111222233");

        List<Transaction> histTxs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(historicalAcc.getAccountId());
        assertThat(histTxs).hasSize(1);
        assertThat(histTxs.get(0).getJournalEntryId()).isNull();
        assertThat(histTxs.get(0).getAmount()).isEqualByComparingTo("500.0000");

        // 4. Verify new account is created properly
        Account reloadedNew = accountRepository.findById(newAccRes.getAccountId()).orElseThrow();
        assertThat(reloadedNew.getBalance()).isEqualByComparingTo("1500.0000");
        List<Transaction> newTxs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(newAccRes.getAccountId());
        assertThat(newTxs).hasSize(1);
        assertThat(newTxs.get(0).getJournalEntryId()).isNotNull();
    }

    // =========================================================================
    // STEP 35: Sole Balance-Writer Regression Guard
    // =========================================================================
    @Test
    void testSoleBalanceWriterArchitectureGuard() throws Exception {
        // Scans all .java files under backend/corebank/src/main/
        Path srcMain = Path.of("src/main/java");
        if (!Files.exists(srcMain)) {
            srcMain = Path.of("backend/corebank/src/main/java");
        }

        Pattern setBalancePattern = Pattern.compile("\\.setBalance\\s*\\(");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(srcMain)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                String fileName = path.getFileName().toString();
                // Allow PostingEngineImpl, Account (entity), and DTOs
                if (fileName.equals("PostingEngineImpl.java") ||
                        fileName.equals("Account.java") ||
                        fileName.endsWith("DTO.java")) {
                    return;
                }

                try {
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        // Exclude DTO mapping setters (e.g. dto.setBalance)
                        if (line.contains("dto.setBalance") || line.contains("DTO")) {
                            continue;
                        }
                        if (setBalancePattern.matcher(line).find() && !line.trim().startsWith("//")) {
                            violations.add(path + ":" + (i + 1) + " -> " + line.trim());
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertThat(violations)
                .as("No production class except PostingEngineImpl may call setBalance on Account")
                .isEmpty();
    }
}
