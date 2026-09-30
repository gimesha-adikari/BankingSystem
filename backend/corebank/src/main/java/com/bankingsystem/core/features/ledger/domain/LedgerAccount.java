package com.bankingsystem.core.features.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping for {@code ledger_accounts} table introduced in V2.
 *
 * <p>Supports both customer accounts (having {@code customer_account_id}) and
 * system accounts (having {@code system_code}), enforced by {@code chk_ledger_accounts_identity}.
 */
@Entity
@Table(name = "ledger_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ledger_accounts_customer", columnNames = "customer_account_id"),
        @UniqueConstraint(name = "uk_ledger_accounts_system_code", columnNames = "system_code")
})
@Getter
@Setter
@NoArgsConstructor
public class LedgerAccount {

    @Id
    @Column(name = "ledger_account_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID ledgerAccountId;

    @Column(name = "customer_account_id", columnDefinition = "BINARY(16)")
    private UUID customerAccountId;

    @Column(name = "system_code", length = 50)
    private String systemCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_class", nullable = false, length = 20)
    private LedgerAccountClass accountClass;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LedgerAccountStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LedgerAccount(UUID ledgerAccountId, UUID customerAccountId, String systemCode,
                         LedgerAccountClass accountClass, String currency,
                         LedgerAccountStatus status, LocalDateTime createdAt) {
        this.ledgerAccountId = ledgerAccountId;
        this.customerAccountId = customerAccountId;
        this.systemCode = systemCode;
        this.accountClass = accountClass;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public boolean isCustomerAccount() {
        return customerAccountId != null;
    }

    public boolean isSystemAccount() {
        return systemCode != null;
    }
}
