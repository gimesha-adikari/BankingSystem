package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.TransactionResponseDTO;

import java.util.List;
import java.util.UUID;

public interface TransactionQueryService {
    List<TransactionResponseDTO> getTransactionsForAccount(UUID accountId);
}
