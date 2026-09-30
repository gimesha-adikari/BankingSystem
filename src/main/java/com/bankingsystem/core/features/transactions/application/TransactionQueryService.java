package com.bankingsystem.core.features.transactions.application;

import com.bankingsystem.core.features.transactions.interfaces.dto.TransactionResponseDTO;

import java.util.List;
import java.util.UUID;

public interface TransactionQueryService {
    List<TransactionResponseDTO> getTransactionsForAccount(UUID accountId);
}
