package com.bankingsystem.core.features.auth.domain;

import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.employee.domain.Employee;
import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username"}),
        @UniqueConstraint(columnNames = {"email"})
})
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "first_name", nullable = true)
    private String firstName;

    @Column(name = "last_name", nullable = true)
    private String lastName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = true)
    @JsonManagedReference
    private Employee employee;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", columnDefinition = "TEXT")
    private String city;

    @Column(name = "state", columnDefinition = "TEXT")
    private String state;

    @Column(name = "country", columnDefinition = "TEXT")
    private String country;

    @Column(name = "postal_code", columnDefinition = "TEXT")
    private String postalCode;

    @Column(name = "home_number", columnDefinition = "TEXT")
    private String homeNumber;

    @Column(name = "work_number", columnDefinition = "TEXT")
    private String workNumber;

    @Column(name = "office_number", columnDefinition = "TEXT")
    private String officeNumber;

    @Column(name = "mobile_number", columnDefinition = "TEXT")
    private String mobileNumber;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "new_email", unique = true, length = 150)
    private String newEmail;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_token_created_at")
    private LocalDateTime emailVerificationTokenCreatedAt;
}
