package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.AccountRequestDTO;
import com.bankingsystem.core.dto.AccountResponseDTO;
import com.bankingsystem.core.entity.Account;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    List<AccountResponseDTO> getAllAccounts();

    AccountResponseDTO getAccountById(UUID accountId);

    List<AccountResponseDTO> getAccountsByCustomerId(UUID customerId);

    AccountResponseDTO openAccount(AccountRequestDTO request, UUID customerId);

    AccountResponseDTO updateAccount(UUID accountId, AccountRequestDTO updatedAccount);

    void closeAccount(UUID accountId);

    AccountResponseDTO changeAccountStatus(UUID accountId, Account.AccountStatus status);

    List<AccountResponseDTO> getAccountsForCurrentUser();
}
