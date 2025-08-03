package com.bankingsystem.core.repository;

import com.bankingsystem.core.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, Integer> {
    Optional<Branch> findById(Integer id);
    Optional<Branch> findByBranchName(String branchName);
}
