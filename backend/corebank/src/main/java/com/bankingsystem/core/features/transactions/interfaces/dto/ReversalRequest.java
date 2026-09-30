package com.bankingsystem.core.features.transactions.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/** HTTP input for a complete reversal; accounting data is loaded from the original journal. */
public record ReversalRequest(@NotBlank(message = "Reason is required") String reason) {
}
