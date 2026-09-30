package com.bankingsystem.core.features.transactions;

import com.bankingsystem.core.features.transactions.domain.Transaction;
import com.bankingsystem.core.features.transactions.interfaces.dto.TransactionResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void transactionResponseDtoSerializesAsDirectArray() throws Exception {
        TransactionResponseDTO dto1 = new TransactionResponseDTO();
        dto1.setTransactionId(UUID.randomUUID());
        dto1.setAccountId(UUID.randomUUID());
        dto1.setType(Transaction.TransactionType.DEPOSIT);
        dto1.setAmount(new BigDecimal("100.0000"));
        dto1.setBalanceAfter(new BigDecimal("100.0000"));
        dto1.setDescription("Deposit");
        dto1.setCreatedAt(LocalDateTime.now());

        TransactionResponseDTO dto2 = new TransactionResponseDTO();
        dto2.setTransactionId(UUID.randomUUID());
        dto2.setAccountId(UUID.randomUUID());
        dto2.setType(Transaction.TransactionType.WITHDRAWAL);
        dto2.setAmount(new BigDecimal("40.0000"));
        dto2.setBalanceAfter(new BigDecimal("60.0000"));
        dto2.setDescription("Withdrawal");
        dto2.setCreatedAt(LocalDateTime.now());

        List<TransactionResponseDTO> list = List.of(dto1, dto2);

        String json = objectMapper.writeValueAsString(list);

        // Must start with '[' and end with ']' (direct JSON array, not wrapped in { data: [...] } or { content: [...] })
        assertThat(json.trim()).startsWith("[").endsWith("]");

        List<TransactionResponseDTO> deserialized = objectMapper.readValue(json, new TypeReference<List<TransactionResponseDTO>>() {});
        assertThat(deserialized).hasSize(2);
        assertThat(deserialized.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(deserialized.get(1).getBalanceAfter()).isEqualByComparingTo(new BigDecimal("60.0000"));
    }
}
