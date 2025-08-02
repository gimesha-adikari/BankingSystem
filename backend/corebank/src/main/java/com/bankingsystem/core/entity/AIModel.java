package com.bankingsystem.core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_models")
public class AIModel {

    @Id
    @GeneratedValue
    @Column(name = "ai_model_id", columnDefinition = "BINARY(16)")
    private UUID aiModelId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_trained_at")
    private LocalDateTime lastTrainedAt;

    // Getters and setters

    public UUID getAiModelId() {
        return aiModelId;
    }

    public void setAiModelId(UUID aiModelId) {
        this.aiModelId = aiModelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastTrainedAt() {
        return lastTrainedAt;
    }

    public void setLastTrainedAt(LocalDateTime lastTrainedAt) {
        this.lastTrainedAt = lastTrainedAt;
    }

    // Enum for Status
    public enum Status {
        ACTIVE,
        DEPRECATED
    }
}
