package com.bankingsystem.core.features.ledger.application.impl;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.ledger.application.PostingCommand;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.application.PostingInstruction;
import com.bankingsystem.core.features.ledger.application.PostingResult;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.JournalEntryRepository;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.transactions.application.LegacyTransactionProjectionService;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingEngineImpl implements PostingEngine {

    private static final BigDecimal MAX_DECIMAL_19_4 = new BigDecimal("999999999999999.9999");
    public static final Comparator<UUID> CANONICAL_UUID_ORDER = Comparator.naturalOrder();

    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalPostingRepository journalPostingRepository;
    private final AccountRepository accountRepository;
    private final LegacyTransactionProjectionService legacyTransactionProjectionService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public PostingResult post(PostingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Posting command cannot be null");
        }

        // 1. Validate entry type for runtime execution
        JournalEntryType type = command.getEntryType();
        if (type == JournalEntryType.OPENING_BALANCE) {
            throw new BusinessException("ERR_INVALID_ENTRY_TYPE",
                    "OPENING_BALANCE entries are reserved exclusively for migration cutover");
        }
        if (type == JournalEntryType.REVERSAL) {
            throw new BusinessException("ERR_INVALID_ENTRY_TYPE",
                    "REVERSAL entries are reserved for dedicated reversal workflows");
        }

        List<PostingInstruction> instructions = command.getInstructions();
        if (instructions == null || instructions.size() < 2) {
            throw new IllegalArgumentException("Posting command must have at least 2 instructions");
        }

        // 2. Validate instructions, sum debits and credits, verify currency harmonization
        BigDecimal totalDebits = BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        for (PostingInstruction inst : instructions) {
            if (inst == null) {
                throw new IllegalArgumentException("Posting instruction cannot be null");
            }
            if (!inst.getAmount().getCurrency().equals(command.getCurrency())) {
                throw new BusinessException("ERR_CURRENCY_MISMATCH",
                        "Posting instruction currency " + inst.getAmount().getCurrency() +
                                " does not match command currency " + command.getCurrency());
            }
            if (inst.getDirection() == PostingDirection.DEBIT) {
                totalDebits = totalDebits.add(inst.getAmount().getAmount());
            } else if (inst.getDirection() == PostingDirection.CREDIT) {
                totalCredits = totalCredits.add(inst.getAmount().getAmount());
            } else {
                throw new IllegalArgumentException("Unknown posting direction: " + inst.getDirection());
            }
        }

        // Invariant: debits must exactly equal credits
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException("ERR_UNBALANCED_JOURNAL",
                    "Total debits (" + totalDebits + ") must equal total credits (" + totalCredits + ")");
        }

        // Invariant: total amount must fit in DECIMAL(19,4)
        if (totalDebits.compareTo(MAX_DECIMAL_19_4) > 0) {
            throw new BusinessException("ERR_AMOUNT_OVERFLOW",
                    "Total amount " + totalDebits + " exceeds maximum allowed DECIMAL(19,4)");
        }

        // 3. Load and validate LedgerAccounts
        Map<UUID, LedgerAccount> loadedLedgerAccounts = new HashMap<>();
        for (PostingInstruction inst : instructions) {
            UUID laId = inst.getLedgerAccountId();
            if (!loadedLedgerAccounts.containsKey(laId)) {
                LedgerAccount la = ledgerAccountRepository.findById(laId)
                        .orElseThrow(() -> new BusinessException("ERR_LEDGER_ACCOUNT_NOT_FOUND",
                                "Ledger account not found: " + laId));
                if (la.getStatus() != LedgerAccountStatus.ACTIVE) {
                    throw new BusinessException("ERR_LEDGER_ACCOUNT_NOT_ACTIVE",
                            "Ledger account " + laId + " is not ACTIVE (status=" + la.getStatus() + ")");
                }
                if (!la.getCurrency().equals(command.getCurrency().getCode())) {
                    throw new BusinessException("ERR_CURRENCY_MISMATCH",
                            "Ledger account " + laId + " currency (" + la.getCurrency() +
                                    ") does not match command currency (" + command.getCurrency().getCode() + ")");
                }
                // Customer ledger account invariant: customer accounts MUST be LIABILITY class
                if (la.isCustomerAccount() && la.getAccountClass() != LedgerAccountClass.LIABILITY) {
                    throw new BusinessException("ERR_CORRUPT_LEDGER_MAPPING",
                            "Customer ledger account " + laId + " must be LIABILITY class, found: " + la.getAccountClass());
                }
                loadedLedgerAccounts.put(laId, la);
            }
        }

        // 4. Collect affected customer account IDs and sort using canonical UUID ordering
        Set<UUID> affectedCustomerAccountIds = new HashSet<>();
        for (PostingInstruction inst : instructions) {
            LedgerAccount la = loadedLedgerAccounts.get(inst.getLedgerAccountId());
            if (la.isCustomerAccount()) {
                affectedCustomerAccountIds.add(la.getCustomerAccountId());
            }
        }

        List<UUID> sortedCustomerAccountIds = new ArrayList<>(affectedCustomerAccountIds);
        sortedCustomerAccountIds.sort(CANONICAL_UUID_ORDER);

        // 5. Acquire PESSIMISTIC_WRITE lock on customer accounts in canonical order
        Map<UUID, Account> lockedCustomerAccounts = new HashMap<>();
        for (UUID customerAccountId : sortedCustomerAccountIds) {
            Account account = accountRepository.findByIdForUpdate(customerAccountId)
                    .orElseThrow(() -> new BusinessException("ERR_CUSTOMER_ACCOUNT_NOT_FOUND",
                            "Underlying customer account not found: " + customerAccountId));

            if (!account.getCurrency().equals(command.getCurrency().getCode())) {
                throw new BusinessException("ERR_CURRENCY_MISMATCH",
                        "Customer account " + customerAccountId + " currency (" + account.getCurrency() +
                                ") does not match command currency (" + command.getCurrency().getCode() + ")");
            }

            lockedCustomerAccounts.put(customerAccountId, account);
        }

        // 6. Aggregate balance deltas for each customer account
        Map<UUID, BigDecimal> aggregateDeltas = new HashMap<>();
        for (PostingInstruction inst : instructions) {
            LedgerAccount la = loadedLedgerAccounts.get(inst.getLedgerAccountId());
            if (la.isCustomerAccount()) {
                UUID customerAccountId = la.getCustomerAccountId();
                BigDecimal delta = LedgerMath.balanceDelta(la.getAccountClass(), inst.getDirection(), inst.getAmount().getAmount());
                aggregateDeltas.merge(customerAccountId, delta, BigDecimal::add);
            }
        }

        // 7. Calculate and validate proposed balances; reject if any resulting balance is negative
        Map<UUID, BigDecimal> resultingCustomerBalances = new HashMap<>();
        for (UUID customerAccountId : sortedCustomerAccountIds) {
            Account account = lockedCustomerAccounts.get(customerAccountId);
            BigDecimal delta = aggregateDeltas.getOrDefault(customerAccountId, BigDecimal.ZERO);
            BigDecimal newBalance = account.getBalance().add(delta).setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_FUNDS",
                        "Insufficient funds: account " + account.getAccountNumber() +
                                " current balance " + account.getBalance() +
                                " cannot support debit resulting in " + newBalance);
            }

            account.setBalance(newBalance);
            account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            accountRepository.save(account);
            resultingCustomerBalances.put(customerAccountId, newBalance);
        }

        // 8. Generate unique entry reference
        String entryReference = generateCollisionSafeReference();

        // 9. Persist JournalEntry (Immutable)
        UUID entryId = UUID.randomUUID();
        LocalDateTime postedAt = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

        JournalEntry journalEntry = new JournalEntry(
                entryId,
                entryReference,
                command.getEntryType(),
                JournalEntryStatus.POSTED,
                command.getCurrency().getCode(),
                totalDebits,
                command.getDescription(),
                null,
                command.getActor().getActorType(),
                command.getActor().getUserId(),
                command.getActor().getSystemActorId(),
                command.getChannel(),
                postedAt
        );
        journalEntryRepository.save(journalEntry);

        // 10. Persist JournalPostings (Immutable)
        for (int seq = 0; seq < instructions.size(); seq++) {
            PostingInstruction inst = instructions.get(seq);
            UUID postingId = UUID.randomUUID();
            JournalPosting posting = new JournalPosting(
                    postingId,
                    entryId,
                    inst.getLedgerAccountId(),
                    seq,
                    inst.getDirection(),
                    inst.getAmount().getAmount(),
                    command.getCurrency().getCode(),
                    postedAt
            );
            journalPostingRepository.save(posting);
        }

        // 11. Create legacy Transaction projection rows (synchronous read model)
        legacyTransactionProjectionService.projectTransactions(
                journalEntry,
                lockedCustomerAccounts,
                aggregateDeltas
        );

        // Flush all writes to database so constraint violations / triggers trigger rollback immediately
        entityManager.flush();

        log.info("Posted journal entry ref={}, type={}, amount={} {}",
                entryReference, command.getEntryType(), totalDebits, command.getCurrency());

        return new PostingResult(
                entryId,
                entryReference,
                command.getEntryType(),
                command.getCurrency(),
                totalDebits,
                postedAt,
                resultingCustomerBalances
        );
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
