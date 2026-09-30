package com.bankingsystem.core.features.ledger.application.impl;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.ledger.application.PostingCommand;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.application.PostingInstruction;
import com.bankingsystem.core.features.ledger.application.PostingResult;
import com.bankingsystem.core.features.ledger.application.ReversalCommand;
import com.bankingsystem.core.features.ledger.application.ReversalPostingResult;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.LegacyTransactionProjectionService;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** The single production balance writer for normal postings and reversals. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostingEngineImpl implements PostingEngine {

    private static final BigDecimal MAX_DECIMAL_19_4 = new BigDecimal("999999999999999.9999");
    private static final int DESCRIPTION_MAX_LENGTH = 255;
    private static final String VAULT_SYSTEM_CODE = "SYSTEM_VAULT_CASH:LKR";
    public static final Comparator<UUID> CANONICAL_UUID_ORDER = Comparator.naturalOrder();

    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalPostingRepository journalPostingRepository;
    private final AccountRepository accountRepository;
    private final LegacyTransactionProjectionService legacyTransactionProjectionService;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public PostingResult post(PostingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Posting command cannot be null");
        }
        JournalEntryType type = command.getEntryType();
        if (type == JournalEntryType.OPENING_BALANCE) {
            throw new BusinessException("ERR_INVALID_ENTRY_TYPE",
                    "OPENING_BALANCE entries are reserved exclusively for migration cutover");
        }
        if (type == JournalEntryType.REVERSAL) {
            throw new BusinessException("ERR_INVALID_ENTRY_TYPE",
                    "REVERSAL entries are reserved for dedicated reversal workflows");
        }
        return executePosting(type, command.getCurrency(), command.getDescription(), command.getActor(),
                command.getChannel(), command.getInstructions(), null, null, false);
    }

    @Override
    @Transactional
    public ReversalPostingResult postReversal(ReversalCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Reversal command cannot be null");
        }
        // Lock the original before checking the natural reversal identity.
        JournalEntry original = journalEntryRepository.findByIdForUpdate(command.getOriginalJournalEntryId())
                .orElseThrow(() -> new BusinessException("ERR_REVERSAL_ORIGINAL_NOT_FOUND",
                        "Original journal entry not found: " + command.getOriginalJournalEntryId()));
        validateReversibleOriginal(original);

        Optional<JournalEntry> existing = journalEntryRepository.findByReversalOfEntryId(original.getEntryId());
        if (existing.isPresent()) {
            return new ReversalPostingResult(buildReplayResult(original, existing.get()), true);
        }

        CurrencyCode currency = parseCurrency(original.getCurrency());
        List<JournalPosting> originalPostings = journalPostingRepository
                .findByEntryIdOrderBySequenceNumberAsc(original.getEntryId());
        validateOriginalPostings(original, originalPostings, true);

        List<PostingInstruction> inverseInstructions = new ArrayList<>(originalPostings.size());
        for (JournalPosting posting : originalPostings) {
            PostingDirection inverseDirection = posting.getDirection() == PostingDirection.DEBIT
                    ? PostingDirection.CREDIT : PostingDirection.DEBIT;
            inverseInstructions.add(new PostingInstruction(
                    posting.getLedgerAccountId(), inverseDirection,
                    MonetaryAmount.fromLedger(posting.getAmount(), currency)));
        }

        String description = composeReversalDescription(original, command.getReason());
        PostingResult result = executePosting(
                JournalEntryType.REVERSAL, currency, description, command.getActor(), command.getChannel(),
                inverseInstructions, original.getEntryId(), original, true);
        return new ReversalPostingResult(result, false);
    }

    /** Shared accounting path. Only this method mutates Account.balance. */
    private PostingResult executePosting(
            JournalEntryType entryType,
            CurrencyCode currency,
            String description,
            PostingActor actor,
            LedgerChannel channel,
            List<PostingInstruction> instructions,
            UUID reversalOfEntryId,
            JournalEntry originalEntry,
            boolean reversalPath) {
        if (instructions == null || instructions.size() < 2) {
            throw new IllegalArgumentException("Posting command must have at least 2 instructions");
        }
        BigDecimal totalDebits = BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
        Map<UUID, LedgerAccount> loadedLedgerAccounts = new HashMap<>();

        for (PostingInstruction instruction : instructions) {
            if (instruction == null) {
                throw new IllegalArgumentException("Posting instruction cannot be null");
            }
            if (!instruction.getAmount().getCurrency().equals(currency)) {
                throw new BusinessException("ERR_CURRENCY_MISMATCH",
                        "Posting instruction currency " + instruction.getAmount().getCurrency()
                                + " does not match command currency " + currency);
            }
            if (instruction.getDirection() == PostingDirection.DEBIT) {
                totalDebits = totalDebits.add(instruction.getAmount().getAmount());
            } else if (instruction.getDirection() == PostingDirection.CREDIT) {
                totalCredits = totalCredits.add(instruction.getAmount().getAmount());
            } else {
                throw new IllegalArgumentException("Unknown posting direction: " + instruction.getDirection());
            }

            UUID ledgerAccountId = instruction.getLedgerAccountId();
            if (!loadedLedgerAccounts.containsKey(ledgerAccountId)) {
                LedgerAccount ledgerAccount = ledgerAccountRepository.findById(ledgerAccountId)
                        .orElseThrow(() -> new BusinessException("ERR_LEDGER_ACCOUNT_NOT_FOUND",
                                "Ledger account not found: " + ledgerAccountId));
                validateLedgerAccountForPosting(ledgerAccount, currency, reversalPath);
                loadedLedgerAccounts.put(ledgerAccountId, ledgerAccount);
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException("ERR_UNBALANCED_JOURNAL",
                    "Total debits (" + totalDebits + ") must equal credits (" + totalCredits + ")");
        }
        if (totalDebits.compareTo(MAX_DECIMAL_19_4) > 0) {
            throw new BusinessException("ERR_AMOUNT_OVERFLOW",
                    "Total amount " + totalDebits + " exceeds maximum allowed DECIMAL(19,4)");
        }
        if (reversalPath && (entryType != JournalEntryType.REVERSAL || reversalOfEntryId == null
                || originalEntry == null)) {
            throw new BusinessException("ERR_INVALID_ENTRY_TYPE", "Invalid dedicated reversal posting context");
        }

        Set<UUID> affectedIds = new HashSet<>();
        for (LedgerAccount ledgerAccount : loadedLedgerAccounts.values()) {
            if (ledgerAccount.isCustomerAccount()) {
                affectedIds.add(ledgerAccount.getCustomerAccountId());
            }
        }
        List<UUID> sortedIds = new ArrayList<>(affectedIds);
        sortedIds.sort(CANONICAL_UUID_ORDER);

        Map<UUID, Account> lockedAccounts = new HashMap<>();
        for (UUID accountId : sortedIds) {
            Account account = accountRepository.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new BusinessException("ERR_CUSTOMER_ACCOUNT_NOT_FOUND",
                            "Underlying customer account not found: " + accountId));
            if (!account.getCurrency().equals(currency.getCode())) {
                throw new BusinessException("ERR_CURRENCY_MISMATCH",
                        "Customer account " + accountId + " currency (" + account.getCurrency()
                                + ") does not match posting currency (" + currency + ")");
            }
            if (reversalPath && account.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new BusinessException("ERR_ACCOUNT_NOT_ACTIVE",
                        "Customer account must be ACTIVE for a reversal: " + accountId);
            }
            lockedAccounts.put(accountId, account);
        }

        Map<UUID, BigDecimal> aggregateDeltas = new HashMap<>();
        for (PostingInstruction instruction : instructions) {
            LedgerAccount ledgerAccount = loadedLedgerAccounts.get(instruction.getLedgerAccountId());
            if (ledgerAccount.isCustomerAccount()) {
                aggregateDeltas.merge(ledgerAccount.getCustomerAccountId(),
                        LedgerMath.balanceDelta(ledgerAccount.getAccountClass(), instruction.getDirection(),
                                instruction.getAmount().getAmount()), BigDecimal::add);
            }
        }

        Map<UUID, BigDecimal> resultingBalances = new HashMap<>();
        for (UUID accountId : sortedIds) {
            Account account = lockedAccounts.get(accountId);
            BigDecimal newBalance = account.getBalance()
                    .add(aggregateDeltas.getOrDefault(accountId, BigDecimal.ZERO))
                    .setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_FUNDS",
                        "Insufficient funds: account " + account.getAccountNumber()
                                + " cannot support resulting balance " + newBalance);
            }
            account.setBalance(newBalance);
            account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            accountRepository.save(account);
            resultingBalances.put(accountId, newBalance);
        }

        String entryReference = generateCollisionSafeReference();
        UUID entryId = UUID.randomUUID();
        LocalDateTime postedAt = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        JournalEntry journalEntry = new JournalEntry(
                entryId, entryReference, entryType, JournalEntryStatus.POSTED, currency.getCode(), totalDebits,
                description, reversalOfEntryId, actor.getActorType(), actor.getUserId(), actor.getSystemActorId(),
                channel, postedAt);
        journalEntryRepository.save(journalEntry);

        for (int sequence = 0; sequence < instructions.size(); sequence++) {
            PostingInstruction instruction = instructions.get(sequence);
            journalPostingRepository.save(new JournalPosting(
                    UUID.randomUUID(), entryId, instruction.getLedgerAccountId(), sequence,
                    instruction.getDirection(), instruction.getAmount().getAmount(), currency.getCode(), postedAt));
        }

        if (reversalPath) {
            legacyTransactionProjectionService.projectReversalTransactions(
                    journalEntry, originalEntry, lockedAccounts, aggregateDeltas);
        } else {
            legacyTransactionProjectionService.projectTransactions(journalEntry, lockedAccounts, aggregateDeltas);
        }
        entityManager.flush();
        log.info("Posted journal entry ref={}, type={}, amount={} {}", entryReference, entryType, totalDebits, currency);
        return new PostingResult(entryId, entryReference, entryType, currency, totalDebits, postedAt, resultingBalances);
    }

    private void validateLedgerAccountForPosting(LedgerAccount ledgerAccount, CurrencyCode currency,
                                                  boolean reversalPath) {
        if (ledgerAccount.isCustomerAccount() == ledgerAccount.isSystemAccount()) {
            throw new BusinessException("ERR_CORRUPT_LEDGER_MAPPING",
                    "Ledger account must identify exactly one customer or system account");
        }
        if (ledgerAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new BusinessException("ERR_LEDGER_ACCOUNT_NOT_ACTIVE",
                    "Ledger account " + ledgerAccount.getLedgerAccountId() + " is not ACTIVE");
        }
        if (!currency.getCode().equals(ledgerAccount.getCurrency())) {
            throw new BusinessException("ERR_CURRENCY_MISMATCH",
                    "Ledger account currency does not match posting currency");
        }
        if (ledgerAccount.isCustomerAccount() && ledgerAccount.getAccountClass() != LedgerAccountClass.LIABILITY) {
            throw new BusinessException("ERR_CORRUPT_LEDGER_MAPPING", "Customer ledger account must be LIABILITY");
        }
        if (reversalPath && ledgerAccount.isSystemAccount()
                && (ledgerAccount.getAccountClass() != LedgerAccountClass.ASSET
                || !VAULT_SYSTEM_CODE.equals(ledgerAccount.getSystemCode()))) {
            throw new BusinessException("ERR_CORRUPT_LEDGER_MAPPING",
                    "System reversal account must retain the original active vault identity and ASSET class");
        }
    }

    private void validateReversibleOriginal(JournalEntry original) {
        if (original.getStatus() != JournalEntryStatus.POSTED) {
            throw new BusinessException("ERR_REVERSAL_ORIGINAL_INVALID", "Only POSTED journals can be reversed");
        }
        if (original.getReversalOfEntryId() != null) {
            throw new BusinessException("ERR_REVERSAL_NOT_ALLOWED", "A reversal cannot itself be reversed");
        }
        if (original.getEntryType() != JournalEntryType.DEPOSIT
                && original.getEntryType() != JournalEntryType.WITHDRAWAL
                && original.getEntryType() != JournalEntryType.TRANSFER) {
            throw new BusinessException("ERR_REVERSAL_NOT_ALLOWED",
                    "Journal type is not operationally reversible: " + original.getEntryType());
        }
        if (original.getTotalAmount() == null || original.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw corruptOriginal("Original journal amount is invalid");
        }
    }

    private Map<UUID, LedgerAccount> validateOriginalPostings(JournalEntry original,
                                                                List<JournalPosting> postings,
                                                                boolean requireActive) {
        if (postings == null || postings.isEmpty()) {
            throw corruptOriginal("Original journal has no postings");
        }
        BigDecimal debits = BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE);
        BigDecimal credits = BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE);
        Map<UUID, LedgerAccount> accounts = new HashMap<>();
        Set<Integer> sequences = new HashSet<>();
        for (int i = 0; i < postings.size(); i++) {
            JournalPosting posting = postings.get(i);
            if (posting == null || !original.getEntryId().equals(posting.getEntryId())
                    || posting.getSequenceNumber() == null || posting.getSequenceNumber() != i
                    || !sequences.add(posting.getSequenceNumber()) || posting.getDirection() == null
                    || posting.getAmount() == null || posting.getAmount().compareTo(BigDecimal.ZERO) <= 0
                    || posting.getAmount().scale() > MonetaryAmount.STORAGE_SCALE
                    || !Objects.equals(original.getCurrency(), posting.getCurrency())) {
                throw corruptOriginal("Original posting set is structurally invalid");
            }
            LedgerAccount ledgerAccount = ledgerAccountRepository.findById(posting.getLedgerAccountId())
                    .orElseThrow(() -> corruptOriginal("Original ledger account is missing: " + posting.getLedgerAccountId()));
            validateStoredLedgerIdentity(ledgerAccount, original.getCurrency(), requireActive);
            accounts.put(posting.getLedgerAccountId(), ledgerAccount);
            if (posting.getDirection() == PostingDirection.DEBIT) {
                debits = debits.add(posting.getAmount());
            } else {
                credits = credits.add(posting.getAmount());
            }
        }
        if (debits.compareTo(credits) != 0 || debits.compareTo(original.getTotalAmount()) != 0) {
            throw corruptOriginal("Original posting totals are unbalanced or disagree with journal amount");
        }
        validateOriginalShape(original, postings, accounts);
        return accounts;
    }

    private void validateOriginalShape(JournalEntry original, List<JournalPosting> postings,
                                       Map<UUID, LedgerAccount> accounts) {
        if (postings.size() != 2) {
            throw corruptOriginal("Supported original transaction must have exactly two authoritative postings");
        }
        long customerLegs = postings.stream().filter(p -> accounts.get(p.getLedgerAccountId()).isCustomerAccount()).count();
        long systemLegs = postings.stream().filter(p -> accounts.get(p.getLedgerAccountId()).isSystemAccount()).count();
        boolean valid;
        if (original.getEntryType() == JournalEntryType.DEPOSIT) {
            valid = customerLegs == 1 && systemLegs == 1
                    && postings.stream().anyMatch(p -> accounts.get(p.getLedgerAccountId()).isSystemAccount()
                    && p.getDirection() == PostingDirection.DEBIT)
                    && postings.stream().anyMatch(p -> accounts.get(p.getLedgerAccountId()).isCustomerAccount()
                    && p.getDirection() == PostingDirection.CREDIT);
        } else if (original.getEntryType() == JournalEntryType.WITHDRAWAL) {
            valid = customerLegs == 1 && systemLegs == 1
                    && postings.stream().anyMatch(p -> accounts.get(p.getLedgerAccountId()).isCustomerAccount()
                    && p.getDirection() == PostingDirection.DEBIT)
                    && postings.stream().anyMatch(p -> accounts.get(p.getLedgerAccountId()).isSystemAccount()
                    && p.getDirection() == PostingDirection.CREDIT);
        } else {
            valid = customerLegs == 2 && systemLegs == 0
                    && postings.stream().filter(p -> p.getDirection() == PostingDirection.DEBIT).count() == 1
                    && postings.stream().filter(p -> p.getDirection() == PostingDirection.CREDIT).count() == 1
                    && !Objects.equals(
                    accounts.get(postings.get(0).getLedgerAccountId()).getCustomerAccountId(),
                    accounts.get(postings.get(1).getLedgerAccountId()).getCustomerAccountId());
        }
        if (!valid) {
            throw corruptOriginal("Original posting structure does not match its journal transaction type");
        }
    }

    private void validateStoredLedgerIdentity(LedgerAccount account, String currency, boolean requireActive) {
        if (account.isCustomerAccount() == account.isSystemAccount() || !Objects.equals(currency, account.getCurrency())) {
            throw corruptOriginal("Original ledger-account mapping or currency is invalid");
        }
        if (requireActive && account.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new BusinessException("ERR_LEDGER_ACCOUNT_NOT_ACTIVE",
                    "Original ledger account is no longer ACTIVE: " + account.getLedgerAccountId());
        }
        if (account.isCustomerAccount() && account.getAccountClass() != LedgerAccountClass.LIABILITY) {
            throw corruptOriginal("Original customer ledger account is not LIABILITY");
        }
        if (account.isSystemAccount() && account.getAccountClass() != LedgerAccountClass.ASSET) {
            throw corruptOriginal("Original system ledger account is not ASSET");
        }
        if (account.isSystemAccount() && !VAULT_SYSTEM_CODE.equals(account.getSystemCode())) {
            throw corruptOriginal("Original system ledger account no longer has the expected vault identity");
        }
    }

    private PostingResult buildReplayResult(JournalEntry original, JournalEntry existing) {
        if (existing.getEntryType() != JournalEntryType.REVERSAL
                || existing.getStatus() != JournalEntryStatus.POSTED
                || !Objects.equals(existing.getReversalOfEntryId(), original.getEntryId())
                || !Objects.equals(existing.getCurrency(), original.getCurrency())
                || existing.getTotalAmount() == null
                || existing.getTotalAmount().compareTo(original.getTotalAmount()) != 0) {
            throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal state is malformed");
        }
        List<JournalPosting> originalPostings = journalPostingRepository
                .findByEntryIdOrderBySequenceNumberAsc(original.getEntryId());
        Map<UUID, LedgerAccount> originalAccounts = validateOriginalPostings(original, originalPostings, false);
        List<JournalPosting> reversalPostings = journalPostingRepository
                .findByEntryIdOrderBySequenceNumberAsc(existing.getEntryId());
        if (reversalPostings.size() != originalPostings.size()) {
            throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal postings are incomplete");
        }
        for (int i = 0; i < originalPostings.size(); i++) {
            JournalPosting originalPosting = originalPostings.get(i);
            JournalPosting reversalPosting = reversalPostings.get(i);
            PostingDirection expected = originalPosting.getDirection() == PostingDirection.DEBIT
                    ? PostingDirection.CREDIT : PostingDirection.DEBIT;
            if (reversalPosting.getSequenceNumber() != i
                    || !Objects.equals(reversalPosting.getLedgerAccountId(), originalPosting.getLedgerAccountId())
                    || reversalPosting.getDirection() != expected
                    || reversalPosting.getAmount().compareTo(originalPosting.getAmount()) != 0
                    || !Objects.equals(reversalPosting.getCurrency(), originalPosting.getCurrency())) {
                throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal postings are not inverse");
            }
        }
        Map<UUID, BigDecimal> originalDeltas = calculateCustomerDeltas(originalPostings, originalAccounts);
        List<Transaction> projections = transactionRepository
                .findByJournalEntryIdOrderByCreatedAtAsc(existing.getEntryId());
        Map<UUID, BigDecimal> balances = validateReplayProjections(original, existing, originalDeltas, projections);
        return new PostingResult(existing.getEntryId(), existing.getEntryReference(), existing.getEntryType(),
                parseCurrency(existing.getCurrency()), existing.getTotalAmount(), existing.getPostedAt(), balances);
    }

    private Map<UUID, BigDecimal> validateReplayProjections(JournalEntry original, JournalEntry reversal,
                                                              Map<UUID, BigDecimal> originalDeltas,
                                                              List<Transaction> projections) {
        int expectedCount = original.getEntryType() == JournalEntryType.TRANSFER ? 2 : 1;
        if (projections.size() != expectedCount) {
            throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal projections are incomplete");
        }
        Map<UUID, Transaction.TransactionType> expectedTypes = new HashMap<>();
        for (Map.Entry<UUID, BigDecimal> entry : originalDeltas.entrySet()) {
            Transaction.TransactionType type;
            if (original.getEntryType() == JournalEntryType.DEPOSIT) {
                type = Transaction.TransactionType.WITHDRAWAL;
            } else if (original.getEntryType() == JournalEntryType.WITHDRAWAL) {
                type = Transaction.TransactionType.DEPOSIT;
            } else {
                type = entry.getValue().compareTo(BigDecimal.ZERO) < 0
                        ? Transaction.TransactionType.TRANSFER_IN : Transaction.TransactionType.TRANSFER_OUT;
            }
            expectedTypes.put(entry.getKey(), type);
        }
        Map<UUID, BigDecimal> balances = new HashMap<>();
        for (Transaction projection : projections) {
            if (!Objects.equals(projection.getJournalEntryId(), reversal.getEntryId())
                    || projection.getAccount() == null || projection.getBalanceAfter() == null) {
                throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal projection is malformed");
            }
            UUID accountId = projection.getAccount().getAccountId();
            BigDecimal originalDelta = originalDeltas.get(accountId);
            if (originalDelta == null || expectedTypes.get(accountId) != projection.getType()
                    || projection.getAmount() == null
                    || projection.getAmount().compareTo(originalDelta.abs()) != 0
                    || balances.put(accountId, projection.getBalanceAfter()) != null) {
                throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal projection is inconsistent");
            }
        }
        if (!balances.keySet().equals(expectedTypes.keySet())) {
            throw new BusinessException("ERR_REVERSAL_STATE_INVALID", "Existing reversal projections do not cover affected accounts");
        }
        return balances;
    }

    private Map<UUID, BigDecimal> calculateCustomerDeltas(List<JournalPosting> postings,
                                                            Map<UUID, LedgerAccount> accounts) {
        Map<UUID, BigDecimal> deltas = new HashMap<>();
        for (JournalPosting posting : postings) {
            LedgerAccount account = accounts.get(posting.getLedgerAccountId());
            if (account != null && account.isCustomerAccount()) {
                deltas.merge(account.getCustomerAccountId(),
                        LedgerMath.balanceDelta(account.getAccountClass(), posting.getDirection(), posting.getAmount()),
                        BigDecimal::add);
            }
        }
        return deltas;
    }

    private String composeReversalDescription(JournalEntry original, String reason) {
        String description = "Reversal of " + original.getEntryReference() + ": " + reason;
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException("ERR_REVERSAL_REASON_INVALID",
                    "Reversal reason is too long for the journal description");
        }
        return description;
    }

    private CurrencyCode parseCurrency(String raw) {
        try {
            return CurrencyCode.of(raw);
        } catch (IllegalArgumentException ex) {
            throw corruptOriginal("Original journal currency is invalid: " + raw);
        }
    }

    private BusinessException corruptOriginal(String message) {
        return new BusinessException("ERR_CORRUPT_ORIGINAL", message);
    }

    private String generateCollisionSafeReference() {
        for (int i = 0; i < 5; i++) {
            String ref = "TX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            if (!journalEntryRepository.existsByEntryReference(ref)) {
                return ref;
            }
        }
        throw new BusinessException("ERR_REFERENCE_COLLISION", "Failed to generate unique entry reference");
    }
}
