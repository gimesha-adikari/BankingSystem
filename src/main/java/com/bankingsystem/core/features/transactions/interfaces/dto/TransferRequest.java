package com.bankingsystem.core.features.transactions.interfaces.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferRequest(@NotNull UUID sourceAccountId, @NotNull UUID destinationAccountId, @NotNull @JsonDeserialize(using = StrictDecimalTextDeserializer.class) String amount) {}
