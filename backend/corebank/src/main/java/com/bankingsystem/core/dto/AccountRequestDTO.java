package com.bankingsystem.core.dto;

import com.bankingsystem.core.entity.Account.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public class AccountRequestDTO {
    private AccountType accountType;
    private BigDecimal initialDeposit;
    private Integer branchId;

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountRequestDTO setAccountType(AccountType accountType) {
        this.accountType = accountType;
        return this;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public AccountRequestDTO setBranchId(Integer branchId) {
        this.branchId = branchId;
        return this;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public AccountRequestDTO setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
        return this;
    }
}
