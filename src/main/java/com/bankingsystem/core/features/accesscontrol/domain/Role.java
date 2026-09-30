package com.bankingsystem.core.features.accesscontrol.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
@Entity
@Table(name = "roles", uniqueConstraints = {@UniqueConstraint(columnNames = "role_name")})
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue
    @Column(name = "role_id", columnDefinition = "BINARY(16)")
    private UUID roleId;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "permissions", columnDefinition = "JSON")
    private String permissions;
}

