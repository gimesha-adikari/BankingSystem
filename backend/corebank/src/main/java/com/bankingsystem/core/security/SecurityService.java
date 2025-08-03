package com.bankingsystem.core.security;

import com.bankingsystem.core.entity.Account;
import com.bankingsystem.core.entity.Customer;
import com.bankingsystem.core.repository.AccountRepository;
import com.bankingsystem.core.repository.CustomerRepository;
import com.bankingsystem.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public boolean isAccountOwner(Authentication authentication, UUID accountId) {
        String username = authentication.getName();

        UUID userId = userRepository.findByUsername(username)
                .map(u -> u.getUserId())
                .orElse(null);

        if (userId == null) {
            return false;
        }

        Customer customer = customerRepository.findByUserUserId(userId).orElse(null);
        if (customer == null) {
            return false;
        }

        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return false;
        }

        return account.getCustomer().getCustomerId().equals(customer.getCustomerId());
    }
}
