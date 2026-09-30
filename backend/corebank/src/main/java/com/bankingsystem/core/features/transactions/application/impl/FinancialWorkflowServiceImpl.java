package com.bankingsystem.core.features.transactions.application.impl;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.ledger.application.PostingCommand;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.application.PostingInstruction;
import com.bankingsystem.core.features.ledger.application.PostingResult;
import com.bankingsystem.core.features.ledger.domain.CurrencyCode;
import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.ledger.domain.LedgerAccount;
import com.bankingsystem.core.features.ledger.domain.LedgerAccountClass;
import com.bankingsystem.core.features.ledger.domain.LedgerAccountStatus;
import com.bankingsystem.core.features.ledger.domain.LedgerChannel;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;
import com.bankingsystem.core.features.ledger.domain.PostingActor;
import com.bankingsystem.core.features.ledger.domain.PostingDirection;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.DepositReceipt;
import com.bankingsystem.core.features.transactions.application.DepositWorkflow;
import com.bankingsystem.core.features.transactions.application.TransferReceipt;
import com.bankingsystem.core.features.transactions.application.TransferWorkflow;
import com.bankingsystem.core.features.transactions.application.WithdrawalReceipt;
import com.bankingsystem.core.features.transactions.application.WithdrawalWorkflow;
import com.bankingsystem.core.features.transactions.idempotency.application.CoreIdempotencyCoordinator;
import com.bankingsystem.core.features.transactions.retry.BoundedFinancialTransactionRetryExecutor;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreOperationType;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreRequestFingerprint;
import com.bankingsystem.core.features.transactions.idempotency.domain.IdempotencyKey;
import com.bankingsystem.core.features.transactions.idempotency.domain.IdempotentCommandResult;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Internal financial application workflows.
 *
 * <p>These methods deliberately stop before HTTP. They validate the authenticated
 * user's account authority and current domain state, then compose the idempotency
 * coordinator with the authoritative posting engine. No balance or legacy history
 * mutation is performed here.</p>
 */
@Service
@RequiredArgsConstructor
public class FinancialWorkflowServiceImpl implements DepositWorkflow, WithdrawalWorkflow, TransferWorkflow {

    private static final int SUCCESS_STATUS_CODE = 200;
    private static final String VAULT_SYSTEM_CODE = "SYSTEM_VAULT_CASH:LKR";
    private static final String SUPPORTED_CURRENCY = "LKR";

    private final CoreIdempotencyCoordinator idempotencyCoordinator;
    private final BoundedFinancialTransactionRetryExecutor retryExecutor;
    private final PostingEngine postingEngine;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final ObjectMapper objectMapper;

    @Override
    public DepositReceipt deposit(UUID authenticatedUserId, UUID targetAccountId,
                                  String decimalAmount, String idempotencyKey) {
        UUID userId = requireUuid(authenticatedUserId, "Authenticated user ID");
        UUID accountId = requireUuid(targetAccountId, "Target account ID");
        IdempotencyKey key = requireIdempotencyKey(idempotencyKey);
        MonetaryAmount amount = requireCustomerAmount(decimalAmount);
        String requestHash = CoreRequestFingerprint.forDeposit(accountId, CurrencyCode.LKR, amount);

        IdempotentCommandResult result = retryExecutor.execute("DEPOSIT", () -> idempotencyCoordinator.execute(
                userId,
                CoreOperationType.DEPOSIT,
                key,
                requestHash,
                () -> executeDeposit(userId, accountId, amount)
        ));

        return readDepositReceipt(result);
    }

    @Override
    public WithdrawalReceipt withdraw(UUID authenticatedUserId, UUID targetAccountId,
                                      String decimalAmount, String idempotencyKey) {
        UUID userId = requireUuid(authenticatedUserId, "Authenticated user ID");
        UUID accountId = requireUuid(targetAccountId, "Target account ID");
        IdempotencyKey key = requireIdempotencyKey(idempotencyKey);
        MonetaryAmount amount = requireCustomerAmount(decimalAmount);
        String requestHash = CoreRequestFingerprint.forWithdrawal(accountId, CurrencyCode.LKR, amount);

        IdempotentCommandResult result = retryExecutor.execute("WITHDRAWAL", () -> idempotencyCoordinator.execute(
                userId,
                CoreOperationType.WITHDRAWAL,
                key,
                requestHash,
                () -> executeWithdrawal(userId, accountId, amount)
        ));

        return readWithdrawalReceipt(result);
    }

    @Override
    public TransferReceipt transfer(UUID authenticatedUserId, UUID sourceAccountId,
                                    UUID destinationAccountId, String decimalAmount,
                                    String idempotencyKey) {
        UUID userId = requireUuid(authenticatedUserId, "Authenticated user ID");
        UUID sourceId = requireUuid(sourceAccountId, "Source account ID");
        UUID destinationId = requireUuid(destinationAccountId, "Destination account ID");
        IdempotencyKey key = requireIdempotencyKey(idempotencyKey);
        MonetaryAmount amount = requireCustomerAmount(decimalAmount);

        if (sourceId.equals(destinationId)) {
            throw new BusinessException("ERR_SELF_TRANSFER", "Source and destination accounts must differ");
        }

        String requestHash = CoreRequestFingerprint.forTransfer(
                sourceId, destinationId, CurrencyCode.LKR, amount);

        IdempotentCommandResult result = retryExecutor.execute("TRANSFER", () -> idempotencyCoordinator.execute(
                userId,
                CoreOperationType.TRANSFER,
                key,
                requestHash,
                () -> executeTransfer(userId, sourceId, destinationId, amount)
        ));

        return readTransferReceipt(result);
    }

    private IdempotentCommandResult executeDeposit(UUID userId, UUID accountId, MonetaryAmount amount) {
        Account account = loadOwnedActiveLkrAccount(userId, accountId);
        LedgerAccount customerLedger = loadCustomerLiability(account);
        LedgerAccount vault = loadVault();

        PostingResult posted = postingEngine.post(new PostingCommand(
                JournalEntryType.DEPOSIT,
                CurrencyCode.LKR,
                "Deposit",
                PostingActor.user(userId),
                LedgerChannel.WEB,
                java.util.List.of(
                        new PostingInstruction(vault.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                        new PostingInstruction(customerLedger.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                )
        ));

        BigDecimal resultingBalance = requireResultingBalance(posted, accountId);
        DepositReceipt receipt = new DepositReceipt(
                JournalEntryType.DEPOSIT,
                posted.getEntryId(),
                posted.getEntryReference(),
                accountId,
                amount.getAmount(),
                CurrencyCode.LKR.getCode(),
                resultingBalance,
                posted.getPostedAt(),
                false
        );
        return IdempotentCommandResult.newlyExecuted(
                posted.getEntryId(), SUCCESS_STATUS_CODE, serialize(receipt));
    }

    private IdempotentCommandResult executeWithdrawal(UUID userId, UUID accountId, MonetaryAmount amount) {
        Account account = loadOwnedActiveLkrAccount(userId, accountId);
        LedgerAccount customerLedger = loadCustomerLiability(account);
        LedgerAccount vault = loadVault();

        PostingResult posted = postingEngine.post(new PostingCommand(
                JournalEntryType.WITHDRAWAL,
                CurrencyCode.LKR,
                "Withdrawal",
                PostingActor.user(userId),
                LedgerChannel.WEB,
                java.util.List.of(
                        new PostingInstruction(customerLedger.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                        new PostingInstruction(vault.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                )
        ));

        BigDecimal resultingBalance = requireResultingBalance(posted, accountId);
        WithdrawalReceipt receipt = new WithdrawalReceipt(
                JournalEntryType.WITHDRAWAL,
                posted.getEntryId(),
                posted.getEntryReference(),
                accountId,
                amount.getAmount(),
                CurrencyCode.LKR.getCode(),
                resultingBalance,
                posted.getPostedAt(),
                false
        );
        return IdempotentCommandResult.newlyExecuted(
                posted.getEntryId(), SUCCESS_STATUS_CODE, serialize(receipt));
    }

    private IdempotentCommandResult executeTransfer(UUID userId, UUID sourceId,
                                                    UUID destinationId, MonetaryAmount amount) {
        Map<UUID, Account> lockedAccounts = lockAccountsInCanonicalOrder(sourceId, destinationId);
        Account source = loadOwnedActiveLkrAccount(userId, lockedAccounts.get(sourceId));
        Account destination = validateActiveLkrAccount(lockedAccounts.get(destinationId));
        LedgerAccount sourceLedger = loadCustomerLiability(source);
        LedgerAccount destinationLedger = loadCustomerLiability(destination);

        if (!Objects.equals(source.getCurrency(), destination.getCurrency())) {
            throw new BusinessException("ERR_CURRENCY_MISMATCH",
                    "Transfer accounts must use the same currency");
        }

        PostingResult posted = postingEngine.post(new PostingCommand(
                JournalEntryType.TRANSFER,
                CurrencyCode.LKR,
                "Transfer",
                PostingActor.user(userId),
                LedgerChannel.WEB,
                java.util.List.of(
                        new PostingInstruction(sourceLedger.getLedgerAccountId(), PostingDirection.DEBIT, amount),
                        new PostingInstruction(destinationLedger.getLedgerAccountId(), PostingDirection.CREDIT, amount)
                )
        ));

        BigDecimal sourceBalance = requireResultingBalance(posted, sourceId);
        BigDecimal destinationBalance = requireResultingBalance(posted, destinationId);
        TransferReceipt receipt = new TransferReceipt(
                JournalEntryType.TRANSFER,
                posted.getEntryId(),
                posted.getEntryReference(),
                sourceId,
                destinationId,
                amount.getAmount(),
                CurrencyCode.LKR.getCode(),
                sourceBalance,
                destinationBalance,
                posted.getPostedAt(),
                false
        );
        return IdempotentCommandResult.newlyExecuted(
                posted.getEntryId(), SUCCESS_STATUS_CODE, serialize(receipt));
    }

    private Account loadOwnedActiveLkrAccount(UUID userId, UUID accountId) {
        return loadOwnedActiveLkrAccount(userId, loadActiveLkrAccount(accountId));
    }

    private Account loadOwnedActiveLkrAccount(UUID userId, Account account) {
        account = validateActiveLkrAccount(account);
        Customer owner = customerRepository.findByUserUserId(userId).orElse(null);
        if (owner == null || account.getCustomer() == null
                || !owner.getCustomerId().equals(account.getCustomer().getCustomerId())) {
            throw new BusinessException("ERR_ACCOUNT_ACCESS_DENIED", "Account access denied");
        }
        return account;
    }

    private Account loadActiveLkrAccount(UUID accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId).orElse(null);
        return validateActiveLkrAccount(account);
    }

    private Account validateActiveLkrAccount(Account account) {
        if (account == null) {
            throw new BusinessException("ERR_ACCOUNT_ACCESS_DENIED", "Account access denied");
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("ERR_ACCOUNT_NOT_ACTIVE",
                    "Account must be ACTIVE for a financial operation");
        }
        if (!SUPPORTED_CURRENCY.equals(account.getCurrency())) {
            throw new BusinessException("ERR_UNSUPPORTED_CURRENCY",
                    "Only LKR customer accounts are supported");
        }
        return account;
    }

    private Map<UUID, Account> lockAccountsInCanonicalOrder(UUID firstAccountId, UUID secondAccountId) {
        List<UUID> orderedIds = List.of(firstAccountId, secondAccountId).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        Map<UUID, Account> accounts = new HashMap<>();
        for (UUID accountId : orderedIds) {
            Account account = accountRepository.findByIdForUpdate(accountId).orElse(null);
            if (account == null) {
                throw new BusinessException("ERR_ACCOUNT_ACCESS_DENIED", "Account access denied");
            }
            accounts.put(accountId, account);
        }
        return accounts;
    }

    private LedgerAccount loadCustomerLiability(Account account) {
        LedgerAccount ledgerAccount = ledgerAccountRepository.findByCustomerAccountId(account.getAccountId())
                .orElseThrow(() -> new BusinessException("ERR_CORRUPT_LEDGER_MAPPING",
                        "Customer ledger account mapping is missing"));

        if (!ledgerAccount.isCustomerAccount()
                || ledgerAccount.isSystemAccount()
                || !account.getAccountId().equals(ledgerAccount.getCustomerAccountId())) {
            throw new BusinessException("ERR_CORRUPT_LEDGER_MAPPING",
                    "Customer ledger account identity is invalid");
        }
        if (ledgerAccount.getAccountClass() != LedgerAccountClass.LIABILITY) {
            throw new BusinessException("ERR_CORRUPT_LEDGER_MAPPING",
                    "Customer ledger account must be LIABILITY");
        }
        if (!SUPPORTED_CURRENCY.equals(ledgerAccount.getCurrency())
                || !account.getCurrency().equals(ledgerAccount.getCurrency())) {
            throw new BusinessException("ERR_CURRENCY_MISMATCH",
                    "Customer ledger account currency does not match the account");
        }
        if (ledgerAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new BusinessException("ERR_LEDGER_ACCOUNT_NOT_ACTIVE",
                    "Customer ledger account must be ACTIVE");
        }
        return ledgerAccount;
    }

    private LedgerAccount loadVault() {
        LedgerAccount vault = ledgerAccountRepository.findBySystemCode(VAULT_SYSTEM_CODE)
                .orElseThrow(() -> new BusinessException("ERR_VAULT_ACCOUNT_NOT_FOUND",
                        "System vault cash account not found: " + VAULT_SYSTEM_CODE));
        if (!vault.isSystemAccount()
                || vault.isCustomerAccount()
                || !VAULT_SYSTEM_CODE.equals(vault.getSystemCode())
                || vault.getAccountClass() != LedgerAccountClass.ASSET
                || !SUPPORTED_CURRENCY.equals(vault.getCurrency())
                || vault.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new BusinessException("ERR_VAULT_ACCOUNT_INVALID",
                    "System vault cash account is not a valid active LKR ASSET account");
        }
        return vault;
    }

    private MonetaryAmount requireCustomerAmount(String decimalAmount) {
        try {
            return MonetaryAmount.fromCustomerInput(decimalAmount, CurrencyCode.LKR);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("ERR_AMOUNT_INVALID", ex.getMessage());
        }
    }

    private IdempotencyKey requireIdempotencyKey(String rawKey) {
        try {
            return IdempotencyKey.of(rawKey);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("ERR_IDEMPOTENCY_KEY_INVALID", ex.getMessage());
        }
    }

    private UUID requireUuid(UUID value, String label) {
        return Objects.requireNonNull(value, label + " must not be null");
    }

    private BigDecimal requireResultingBalance(PostingResult posted, UUID accountId) {
        BigDecimal balance = posted.getResultingCustomerBalances().get(accountId);
        if (balance == null) {
            throw new IllegalStateException("Posting result did not contain balance for account " + accountId);
        }
        return balance;
    }

    private String serialize(Object receipt) {
        try {
            return objectMapper.writeValueAsString(receipt);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize financial workflow receipt", ex);
        }
    }

    private DepositReceipt readDepositReceipt(IdempotentCommandResult result) {
        DepositReceipt receipt = read(result, DepositReceipt.class);
        if (receipt.operationType() != JournalEntryType.DEPOSIT) {
            throw new IllegalStateException("Cached receipt operation type is not DEPOSIT");
        }
        return receipt.withReplayed(result.replayed());
    }

    private WithdrawalReceipt readWithdrawalReceipt(IdempotentCommandResult result) {
        WithdrawalReceipt receipt = read(result, WithdrawalReceipt.class);
        if (receipt.operationType() != JournalEntryType.WITHDRAWAL) {
            throw new IllegalStateException("Cached receipt operation type is not WITHDRAWAL");
        }
        return receipt.withReplayed(result.replayed());
    }

    private TransferReceipt readTransferReceipt(IdempotentCommandResult result) {
        TransferReceipt receipt = read(result, TransferReceipt.class);
        if (receipt.operationType() != JournalEntryType.TRANSFER) {
            throw new IllegalStateException("Cached receipt operation type is not TRANSFER");
        }
        return receipt.withReplayed(result.replayed());
    }

    private <T> T read(IdempotentCommandResult result, Class<T> type) {
        if (result.httpStatusCode() != SUCCESS_STATUS_CODE) {
            throw new IllegalStateException("Unexpected cached financial workflow status: "
                    + result.httpStatusCode());
        }
        if (result.responsePayload() == null || result.responsePayload().isBlank()) {
            throw new IllegalStateException("Cached financial workflow receipt is empty");
        }
        try {
            T receipt = objectMapper.readValue(result.responsePayload(), type);
            if (receipt instanceof DepositReceipt deposit
                    && !result.journalEntryId().equals(deposit.journalEntryId())) {
                throw new IllegalStateException("Cached deposit receipt journal ID mismatch");
            }
            if (receipt instanceof WithdrawalReceipt withdrawal
                    && !result.journalEntryId().equals(withdrawal.journalEntryId())) {
                throw new IllegalStateException("Cached withdrawal receipt journal ID mismatch");
            }
            if (receipt instanceof TransferReceipt transfer
                    && !result.journalEntryId().equals(transfer.journalEntryId())) {
                throw new IllegalStateException("Cached transfer receipt journal ID mismatch");
            }
            return receipt;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize cached financial workflow receipt", ex);
        }
    }
}
