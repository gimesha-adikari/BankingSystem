package com.bankingsystem.core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_scores")
public class CreditScore {

    @Id
    @GeneratedValue
    @Column(name = "credit_score_id", columnDefinition = "BINARY(16)")
    private UUID creditScoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDateTime evaluationDate;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Getters and setters

    public UUID getCreditScoreId() {
        return creditScoreId;
    }

    public void setCreditScoreId(UUID creditScoreId) {
        this.creditScoreId = creditScoreId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDateTime getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(LocalDateTime evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
