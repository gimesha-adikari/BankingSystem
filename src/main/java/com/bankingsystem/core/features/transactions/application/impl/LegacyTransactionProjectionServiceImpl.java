package com.bankingsystem.core.features.transactions.application.impl;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.ledger.domain.JournalEntry;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;
import com.bankingsystem.core.features.transactions.application.LegacyTransactionProjectionService;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LegacyTransactionProjectionServiceImpl implements LegacyTransactionProjectionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Transaction> projectTransactions(
            JournalEntry journalEntry,
            Map<UUID, Account> customerAccounts,
            Map<UUID, BigDecimal> netDeltas
    ) {
        Objects.requireNonNull(journalEntry, "journalEntry must not be null");
        Objects.requireNonNull(customerAccounts, "customerAccounts must not be null");
        Objects.requireNonNull(netDeltas, "netDeltas must not be null");

        return switch (journalEntry.getEntryType()) {
            case DEPOSIT -> projectDeposit(journalEntry, customerAccounts, netDeltas);
            case WITHDRAWAL -> projectWithdrawal(journalEntry, customerAccounts, netDeltas);
            case TRANSFER -> projectTransfer(journalEntry, customerAccounts, netDeltas);
            default -> throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "Unsupported entry type for legacy transaction projection: " + journalEntry.getEntryType()
            );
        };
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Transaction> projectReversalTransactions(
            JournalEntry reversalEntry,
            JournalEntry originalEntry,
            Map<UUID, Account> customerAccounts,
            Map<UUID, BigDecimal> netDeltas
    ) {
        Objects.requireNonNull(reversalEntry, "reversalEntry must not be null");
        Objects.requireNonNull(originalEntry, "originalEntry must not be null");
        if (reversalEntry.getEntryType() != com.bankingsystem.core.features.ledger.domain.JournalEntryType.REVERSAL) {
            throw new BusinessException("ERR_UNSUPPORTED_PROJECTION", "Reversal projection requires a REVERSAL journal");
        }
        return switch (originalEntry.getEntryType()) {
            case DEPOSIT -> projectWithdrawal(reversalEntry, customerAccounts, netDeltas);
            case WITHDRAWAL -> projectDeposit(reversalEntry, customerAccounts, netDeltas);
            case TRANSFER -> projectTransfer(reversalEntry, customerAccounts, netDeltas);
            default -> throw new BusinessException("ERR_UNSUPPORTED_PROJECTION",
                    "Unsupported original type for reversal projection: " + originalEntry.getEntryType());
        };
    }

    private List<Transaction> projectDeposit(
            JournalEntry journalEntry,
            Map<UUID, Account> customerAccounts,
            Map<UUID, BigDecimal> netDeltas
    ) {
        if (netDeltas.size() != 1) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "DEPOSIT projection requires exactly one customer account, found " + netDeltas.size()
            );
        }

        UUID accountId = netDeltas.keySet().iterator().next();
        BigDecimal delta = netDeltas.get(accountId);

        // For DEPOSIT, net customer liability delta must be positive (credit)
        if (delta == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "DEPOSIT customer net movement must be positive, found " + delta
            );
        }

        Account account = customerAccounts.get(accountId);
        if (account == null) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "Account entity missing for customer account id: " + accountId
            );
        }

        BigDecimal amount = delta.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setType(Transaction.TransactionType.DEPOSIT);
        tx.setAmount(amount);
        tx.setBalanceAfter(account.getBalance());
        tx.setDescription(journalEntry.getDescription());
        tx.setCreatedAt(journalEntry.getPostedAt());
        tx.setJournalEntryId(journalEntry.getEntryId());

        Transaction saved = transactionRepository.save(tx);
        return List.of(saved);
    }

    private List<Transaction> projectWithdrawal(
            JournalEntry journalEntry,
            Map<UUID, Account> customerAccounts,
            Map<UUID, BigDecimal> netDeltas
    ) {
        if (netDeltas.size() != 1) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "WITHDRAWAL projection requires exactly one customer account, found " + netDeltas.size()
            );
        }

        UUID accountId = netDeltas.keySet().iterator().next();
        BigDecimal delta = netDeltas.get(accountId);

        // For WITHDRAWAL, net customer liability delta must be negative (debit)
        if (delta == null || delta.compareTo(BigDecimal.ZERO) >= 0) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "WITHDRAWAL customer net movement must be negative, found " + delta
            );
        }

        Account account = customerAccounts.get(accountId);
        if (account == null) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "Account entity missing for customer account id: " + accountId
            );
        }

        BigDecimal amount = delta.abs().setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setType(Transaction.TransactionType.WITHDRAWAL);
        tx.setAmount(amount);
        tx.setBalanceAfter(account.getBalance());
        tx.setDescription(journalEntry.getDescription());
        tx.setCreatedAt(journalEntry.getPostedAt());
        tx.setJournalEntryId(journalEntry.getEntryId());

        Transaction saved = transactionRepository.save(tx);
        return List.of(saved);
    }

    private List<Transaction> projectTransfer(
            JournalEntry journalEntry,
            Map<UUID, Account> customerAccounts,
            Map<UUID, BigDecimal> netDeltas
    ) {
        if (netDeltas.size() != 2) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "TRANSFER projection requires exactly two customer accounts, found " + netDeltas.size()
            );
        }

        UUID sourceAccountId = null;
        UUID destAccountId = null;
        BigDecimal sourceDelta = null;
        BigDecimal destDelta = null;

        for (Map.Entry<UUID, BigDecimal> entry : netDeltas.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                if (sourceAccountId != null) {
                    throw new BusinessException(
                            "ERR_UNSUPPORTED_PROJECTION",
                            "TRANSFER projection cannot have multiple debit accounts"
                    );
                }
                sourceAccountId = entry.getKey();
                sourceDelta = entry.getValue();
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                if (destAccountId != null) {
                    throw new BusinessException(
                            "ERR_UNSUPPORTED_PROJECTION",
                            "TRANSFER projection cannot have multiple credit accounts"
                    );
                }
                destAccountId = entry.getKey();
                destDelta = entry.getValue();
            }
        }

        if (sourceAccountId == null || destAccountId == null) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "TRANSFER projection requires one debit account and one credit account"
            );
        }

        if (sourceDelta.abs().compareTo(destDelta) != 0) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "TRANSFER absolute debit amount (" + sourceDelta.abs() + ") must equal credit amount (" + destDelta + ")"
            );
        }

        Account sourceAccount = customerAccounts.get(sourceAccountId);
        Account destAccount = customerAccounts.get(destAccountId);

        if (sourceAccount == null || destAccount == null) {
            throw new BusinessException(
                    "ERR_UNSUPPORTED_PROJECTION",
                    "Account entity missing for transfer participants"
            );
        }

        BigDecimal transferAmount = destDelta.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        // 1. Source account projection: TRANSFER_OUT
        Transaction sourceTx = new Transaction();
        sourceTx.setAccount(sourceAccount);
        sourceTx.setType(Transaction.TransactionType.TRANSFER_OUT);
        sourceTx.setAmount(transferAmount);
        sourceTx.setBalanceAfter(sourceAccount.getBalance());
        sourceTx.setDescription(journalEntry.getDescription());
        sourceTx.setCreatedAt(journalEntry.getPostedAt());
        sourceTx.setJournalEntryId(journalEntry.getEntryId());

        // 2. Destination account projection: TRANSFER_IN
        Transaction destTx = new Transaction();
        destTx.setAccount(destAccount);
        destTx.setType(Transaction.TransactionType.TRANSFER_IN);
        destTx.setAmount(transferAmount);
        destTx.setBalanceAfter(destAccount.getBalance());
        destTx.setDescription(journalEntry.getDescription());
        destTx.setCreatedAt(journalEntry.getPostedAt());
        destTx.setJournalEntryId(journalEntry.getEntryId());

        // Save source then destination individually to allow intermediate failure / trigger testing
        Transaction savedSource = transactionRepository.save(sourceTx);
        Transaction savedDest = transactionRepository.save(destTx);

        return List.of(savedSource, savedDest);
    }
}
