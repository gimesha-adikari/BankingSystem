package com.bankingsystem.core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue
    @Column(name = "feedback_id", columnDefinition = "BINARY(16)")
    private UUID feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "feedback_text", columnDefinition = "TEXT", nullable = false)
    private String feedbackText;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 20)
    private ResponseStatus responseStatus;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // Getters and setters

    public UUID getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(UUID feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    // Enum for ResponseStatus

    public enum ResponseStatus {
        PENDING,
        REVIEWED
    }
}
