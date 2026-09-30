package com.bankingsystem.core.features.branch.domain.repository;

import com.bankingsystem.core.features.branch.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Integer> {
    Optional<Branch> findById(Integer id);
    Optional<Branch> findByBranchName(String branchName);
}
