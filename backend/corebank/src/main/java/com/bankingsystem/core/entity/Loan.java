package com.bankingsystem.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue
    @Column(name = "loan_id", columnDefinition = "BINARY(16)")
    private UUID loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "loan_term", nullable = false)
    private Integer loanTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false, length = 20)
    private PaymentFrequency paymentFrequency;

    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    // Getters and setters

    public UUID getLoanId() {
        return loanId;
    }

    public void setLoanId(UUID loanId) {
        this.loanId = loanId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getLoanTerm() {
        return loanTerm;
    }

    public void setLoanTerm(Integer loanTerm) {
        this.loanTerm = loanTerm;
    }

    public PaymentFrequency getPaymentFrequency() {
        return paymentFrequency;
    }

    public void setPaymentFrequency(PaymentFrequency paymentFrequency) {
        this.paymentFrequency = paymentFrequency;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    // Enums

    public enum LoanType {
        PERSONAL,
        MORTGAGE,
        // add others as needed
    }

    public enum PaymentFrequency {
        MONTHLY,
        QUARTERLY,
        YEARLY
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        PAID
    }
}
