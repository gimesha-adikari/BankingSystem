package com.bankingsystem.core.modules.common.security;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.accounts.domain.repository.AccountRepository;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
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
