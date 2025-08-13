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
        createBranchIfNotExists("Colombo Branch", "45 York Street, Colombo 01, Sri Lanka", "+94112556789");
        createBranchIfNotExists("Kandy Branch", "256 Peradeniya Road, Kandy, Sri Lanka", "+94812334567");
        createBranchIfNotExists("Galle Branch", "78 Main Street, Fort, Galle, Sri Lanka", "+94912245678");
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
