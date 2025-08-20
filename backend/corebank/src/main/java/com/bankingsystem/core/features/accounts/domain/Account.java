package com.bankingsystem.core.features.accounts.domain;

import com.bankingsystem.core.features.branch.domain.Branch;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.modules.common.enums.AccountStatus;
import com.bankingsystem.core.modules.common.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = "account_number")
})
public class Account {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "account_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID accountId;


    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = true)
    private Branch branch;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
