package com.bankingsystem.core.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(name = "address", columnDefinition = "TEXT", nullable = false)
    private String address;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_employee_id")
    private Employee manager;

    // Getters and setters

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }
}
