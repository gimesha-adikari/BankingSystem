package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.dto.TransactionResponseDTO;
import com.bankingsystem.core.entity.Account;
import com.bankingsystem.core.entity.Transaction;
import com.bankingsystem.core.exceptions.ResourceNotFoundException;
import com.bankingsystem.core.repository.AccountRepository;
import com.bankingsystem.core.repository.TransactionRepository;
import com.bankingsystem.core.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<TransactionResponseDTO> getTransactionsForAccount(UUID accountId) {
        // Ensure account exists (keeps 404 semantics clean)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(account.getAccountId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TransactionResponseDTO mapToDTO(Transaction t) {
        TransactionResponseDTO dto = new TransactionResponseDTO();
        dto.setTransactionId(t.getTransactionId());
        dto.setAccountId(t.getAccount().getAccountId());
        dto.setType(t.getType());
        dto.setAmount(t.getAmount());
        dto.setBalanceAfter(t.getBalanceAfter());
        dto.setDescription(t.getDescription());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}
