package com.bankingsystem.core.features.accounts.interfaces.dto;

import com.bankingsystem.core.features.accounts.domain.Account.AccountType;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequestDTO {
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    @NotNull(message = "Initial deposit is required") @DecimalMin(value = "0.00", message = "Initial deposit should be greater than 0")
    private BigDecimal initialDeposit;
    @NotNull(message = "Branch ID is required")
    private Integer branchId;

}
