package com.bankingsystem.core.features.accounts.application.impl;

import com.bankingsystem.core.features.accounts.interfaces.dto.AccountRequestDTO;
import com.bankingsystem.core.features.accounts.interfaces.dto.AccountResponseDTO;
import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.branch.domain.Branch;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.modules.common.exceptions.BusinessException;
import com.bankingsystem.core.modules.common.exceptions.ResourceNotFoundException;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.branch.domain.repository.BranchRepository;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.transactions.domain.repository.TransactionRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.accounts.application.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
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
    private final TransactionRepository transactionRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final int MAX_RETRIES = 10;

    private static final Map<Account.AccountType, BigDecimal> MIN_DEPOSIT = Map.of(
            Account.AccountType.SAVINGS, new BigDecimal("1000.00"),
            Account.AccountType.CHECKING, new BigDecimal("0.00"),
            Account.AccountType.FIXED_DEPOSIT, new BigDecimal("5000.00")
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

        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(Account.AccountStatus.ACTIVE);
        account.setBalance(deposit);
        account.setCustomer(customer);
        account.setBranch(branch);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        account = accountRepository.save(account);

        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            Transaction t = new Transaction();
            t.setAccount(account);
            t.setType(Transaction.TransactionType.DEPOSIT);
            t.setAmount(deposit);
            t.setBalanceAfter(account.getBalance());
            t.setDescription("Opening deposit");
            t.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(t);
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
        account.setAccountStatus(Account.AccountStatus.CLOSED);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    @Override
    public AccountResponseDTO changeAccountStatus(UUID accountId, Account.AccountStatus status) {
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
