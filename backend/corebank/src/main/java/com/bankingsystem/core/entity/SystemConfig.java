package com.bankingsystem.core.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "system_configs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"config_key"})
})
public class SystemConfig {

    @Id
    @GeneratedValue
    @Column(name = "config_id", columnDefinition = "BINARY(16)")
    private UUID configId;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT", nullable = false)
    private String configValue;

    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Getters and setters

    public UUID getConfigId() {
        return configId;
    }

    public void setConfigId(UUID configId) {
        this.configId = configId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
