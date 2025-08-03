package com.bankingsystem.core.config;

import com.bankingsystem.core.entity.Branch;
import com.bankingsystem.core.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
public class BranchSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;

    @Override
    public void run(String... args) {
        createBranchIfNotExists("Main Branch", "123 Main St, City", "0123456789");
        createBranchIfNotExists("Downtown Branch", "456 Downtown Ave, City", "0987654321");
        createBranchIfNotExists("Uptown Branch", "789 Uptown Blvd, City", "0112233445");
        // Add more branches as needed
    }

    private void createBranchIfNotExists(String branchName, String address, String contactNumber) {
        branchRepository.findByBranchName(branchName).orElseGet(() -> {
            Branch branch = new Branch();
            branch.setBranchName(branchName);
            branch.setAddress(address);
            branch.setContactNumber(contactNumber);
            // manager can be set later or null here
            return branchRepository.save(branch);
        });
    }
}
