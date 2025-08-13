package com.bankingsystem.core.dto;

import com.bankingsystem.core.entity.Account.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequestDTO {
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal initialDeposit;
    private Integer branchId;

}
