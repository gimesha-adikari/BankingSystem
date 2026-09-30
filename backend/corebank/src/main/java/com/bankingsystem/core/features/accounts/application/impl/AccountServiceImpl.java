package com.bankingsystem.core.features.accounts.application.impl;

import com.bankingsystem.core.features.accounts.interfaces.dto.AccountRequestDTO;
import com.bankingsystem.core.features.accounts.interfaces.dto.AccountResponseDTO;
import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.branch.domain.Branch;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.ledger.application.PostingCommand;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.application.PostingInstruction;
import com.bankingsystem.core.features.ledger.domain.*;
import com.bankingsystem.core.features.ledger.domain.repository.LedgerAccountRepository;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import com.bankingsystem.core.modules.common.exceptions.ResourceNotFoundException;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.branch.domain.repository.BranchRepository;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.accounts.application.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final PostingEngine postingEngine;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final int MAX_RETRIES = 10;

    private static final Map<AccountType, BigDecimal> MIN_DEPOSIT = Map.of(
            AccountType.SAVINGS, new BigDecimal("1000.00"),
            AccountType.CHECKING, new BigDecimal("0.00"),
            AccountType.FIXED_DEPOSIT, new BigDecimal("5000.00")
    );

    @Override
    public List<AccountResponseDTO> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public AccountResponseDTO getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return mapToDTO(account);
    }

    @Override
    public List<AccountResponseDTO> getAccountsByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerCustomerId(customerId)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponseDTO openAccount(AccountRequestDTO request, UUID targetUserId) {
        UUID currentUserId = getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isTeller = "TELLER".equalsIgnoreCase(currentUser.getRole().getRoleName());
        UUID userIdForAccount = isTeller
                ? Optional.ofNullable(targetUserId).orElseThrow(() ->
                new BusinessException("ERR_CUSTOMER_REQUIRED", "Customer Id is required for Teller"))
                : currentUserId;

        Customer customer = customerRepository.findByUserUserId(userIdForAccount)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        BigDecimal deposit = request.getInitialDeposit();
        if (deposit == null || deposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("ERR_DEPOSIT_INVALID", "Initial deposit must be non-negative");
        }
        BigDecimal min = MIN_DEPOSIT.get(request.getAccountType());
        if (min != null && deposit.compareTo(min) < 0) {
            throw new BusinessException("ERR_MIN_DEPOSIT", "Minimum initial deposit for " +
                    request.getAccountType() + " is " + min);
        }

        // Validate customer monetary amount format (max 2 fractional digits, no silent rounding)
        MonetaryAmount monetaryDeposit = null;
        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            try {
                monetaryDeposit = MonetaryAmount.fromCustomerInput(deposit.toPlainString(), CurrencyCode.LKR);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("ERR_DEPOSIT_INVALID", e.getMessage());
            }
        }

        // If funded opening deposit, resolve and validate counterparty SYSTEM_VAULT_CASH:LKR
        LedgerAccount vaultAccount = null;
        if (monetaryDeposit != null) {
            vaultAccount = ledgerAccountRepository.findBySystemCode("SYSTEM_VAULT_CASH:LKR")
                    .orElseThrow(() -> new BusinessException("ERR_VAULT_ACCOUNT_NOT_FOUND",
                            "System vault cash account not found: SYSTEM_VAULT_CASH:LKR"));

            if (vaultAccount.getAccountClass() != LedgerAccountClass.ASSET) {
                throw new BusinessException("ERR_VAULT_ACCOUNT_INVALID",
                        "System vault account must be ASSET class, found: " + vaultAccount.getAccountClass());
            }
            if (!"LKR".equals(vaultAccount.getCurrency())) {
                throw new BusinessException("ERR_VAULT_ACCOUNT_INVALID",
                        "System vault account currency must be LKR, found: " + vaultAccount.getCurrency());
            }
            if (vaultAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
                throw new BusinessException("ERR_VAULT_ACCOUNT_INVALID",
                        "System vault account is not ACTIVE: " + vaultAccount.getStatus());
            }
            if (vaultAccount.getCustomerAccountId() != null) {
                throw new BusinessException("ERR_VAULT_ACCOUNT_INVALID",
                        "System vault account must not be linked to a customer account");
            }
        }

        // 1. Create Account with structural ZERO balance
        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setCurrency("LKR");
        account.setCustomer(customer);
        account.setBranch(branch);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        account = accountRepository.saveAndFlush(account);

        // 2. Create customer liability LedgerAccount
        LedgerAccount customerLedgerAccount = new LedgerAccount(
                UUID.randomUUID(),
                account.getAccountId(),
                null,
                LedgerAccountClass.LIABILITY,
                "LKR",
                LedgerAccountStatus.ACTIVE,
                now
        );
        customerLedgerAccount = ledgerAccountRepository.saveAndFlush(customerLedgerAccount);

        // 3. If funded opening deposit, execute balanced posting via PostingEngine
        if (monetaryDeposit != null) {
            PostingCommand command = new PostingCommand(
                    JournalEntryType.DEPOSIT,
                    CurrencyCode.LKR,
                    "Opening deposit",
                    PostingActor.user(currentUserId),
                    LedgerChannel.WEB,
                    List.of(
                            new PostingInstruction(vaultAccount.getLedgerAccountId(), PostingDirection.DEBIT, monetaryDeposit),
                            new PostingInstruction(customerLedgerAccount.getLedgerAccountId(), PostingDirection.CREDIT, monetaryDeposit)
                    )
            );
            postingEngine.post(command);
            account = accountRepository.findById(account.getAccountId()).orElseThrow();
        }

        return mapToDTO(account);
    }

    @Override
    public AccountResponseDTO updateAccount(UUID accountId, AccountRequestDTO updated) {
        Account existing = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (updated.getAccountType() != null) {
            existing.setAccountType(updated.getAccountType());
        }
        if (updated.getBranchId() != null) {
            Branch branch = branchRepository.findById(updated.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            existing.setBranch(branch);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(accountRepository.save(existing));
    }

    @Override
    public void closeAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setAccountStatus(AccountStatus.CLOSED);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    @Override
    public AccountResponseDTO changeAccountStatus(UUID accountId, AccountStatus status) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setAccountStatus(status);
        account.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(accountRepository.save(account));
    }

    @Override
    public List<AccountResponseDTO> getAccountsForCurrentUser() {
        UUID userId = getCurrentUserId();
        Customer customer = customerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return accountRepository.findByCustomerCustomerId(customer.getCustomerId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // Helpers

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getUserId();
    }

    private AccountResponseDTO mapToDTO(Account account) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountType(account.getAccountType());
        dto.setAccountStatus(account.getAccountStatus());
        dto.setBalance(account.getBalance());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }

    private String generateUniqueAccountNumber() {
        for (int attempts = 0; attempts < MAX_RETRIES; attempts++) {
            String accountNumber = randomDigits(ACCOUNT_NUMBER_LENGTH);
            if (!accountRepository.existsByAccountNumber(accountNumber)) return accountNumber;
        }
        throw new IllegalStateException("Failed to generate unique account number after " + MAX_RETRIES + " attempts");
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(SECURE_RANDOM.nextInt(10));
        return sb.toString();
    }
}
