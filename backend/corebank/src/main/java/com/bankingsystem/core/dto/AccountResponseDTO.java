package com.bankingsystem.core.dto;

import com.bankingsystem.core.entity.Account.AccountStatus;
import com.bankingsystem.core.entity.Account.AccountType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountResponseDTO {
    private UUID accountId;
    private String accountNumber;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private BigDecimal balance;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;


    public UUID getAccountId() {
        return accountId;
    }

    public AccountResponseDTO setAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountResponseDTO setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountResponseDTO setAccountType(AccountType accountType) {
        this.accountType = accountType;
        return this;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public AccountResponseDTO setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        return this;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountResponseDTO setBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AccountResponseDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public AccountResponseDTO setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
