package com.bankingsystem.core.features.ledger.application.impl;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationResult;
import com.bankingsystem.core.features.ledger.application.LedgerReconciliationService;
import com.bankingsystem.core.features.ledger.domain.LedgerAccount;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;
import com.bankingsystem.core.features.ledger.domain.repository.JournalPostingRepository;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerReconciliationServiceImpl implements LedgerReconciliationService {

    private final AccountRepository accountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalPostingRepository journalPostingRepository;

    @Override
    @Transactional(readOnly = true)
    public LedgerReconciliationResult reconcileAll() {
        List<Account> accounts = accountRepository.findAll();
        List<LedgerReconciliationResult.AccountDiscrepancy> discrepancies = new ArrayList<>();
        int reconciledCount = 0;

        for (Account account : accounts) {
            boolean matches = checkAccount(account, discrepancies);
            if (matches) {
                reconciledCount++;
            }
        }

        return new LedgerReconciliationResult(accounts.size(), reconciledCount, discrepancies);
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerReconciliationResult reconcileAccount(UUID customerAccountId) {
        Account account = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Customer account not found: " + customerAccountId));

        List<LedgerReconciliationResult.AccountDiscrepancy> discrepancies = new ArrayList<>();
        boolean matches = checkAccount(account, discrepancies);

        return new LedgerReconciliationResult(1, matches ? 1 : 0, discrepancies);
    }

    private boolean checkAccount(Account account, List<LedgerReconciliationResult.AccountDiscrepancy> discrepancies) {
        UUID accountId = account.getAccountId();
        BigDecimal materialized = account.getBalance().setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        Optional<LedgerAccount> optLa = ledgerAccountRepository.findByCustomerAccountId(accountId);
        if (optLa.isEmpty()) {
            // Customer account has no ledger account mapping
            BigDecimal diff = materialized;
            discrepancies.add(new LedgerReconciliationResult.AccountDiscrepancy(
                    accountId, account.getAccountNumber(), materialized, BigDecimal.ZERO.setScale(MonetaryAmount.STORAGE_SCALE), diff));
            log.warn("Reconciliation discrepancy: account {} has no ledger account mapping", accountId);
            return false;
        }

        LedgerAccount la = optLa.get();
        BigDecimal derived = journalPostingRepository.calculateDerivedLiabilityBalance(la.getLedgerAccountId())
                .setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        if (materialized.compareTo(derived) != 0) {
            BigDecimal diff = materialized.subtract(derived);
            discrepancies.add(new LedgerReconciliationResult.AccountDiscrepancy(
                    accountId, account.getAccountNumber(), materialized, derived, diff));
            log.warn("Reconciliation discrepancy: account {} materialized={}, derived={}, diff={}",
                    account.getAccountNumber(), materialized, derived, diff);
            return false;
        }

        return true;
    }
}
