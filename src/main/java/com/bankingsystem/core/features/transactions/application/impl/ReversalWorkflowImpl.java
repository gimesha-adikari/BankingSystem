package com.bankingsystem.core.features.transactions.application.impl;

import com.bankingsystem.core.features.ledger.application.PostingResult;
import com.bankingsystem.core.features.ledger.application.ReversalCommand;
import com.bankingsystem.core.features.ledger.application.ReversalPostingResult;
import com.bankingsystem.core.features.ledger.application.PostingEngine;
import com.bankingsystem.core.features.ledger.domain.LedgerChannel;
import com.bankingsystem.core.features.ledger.domain.PostingActor;
import com.bankingsystem.core.features.transactions.application.ReversalReceipt;
import com.bankingsystem.core.features.transactions.application.ReversalWorkflow;
import com.bankingsystem.core.features.transactions.retry.BoundedFinancialTransactionRetryExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/** Executes the complete reversal in bounded fresh transaction attempts. */
@Service
@RequiredArgsConstructor
public class ReversalWorkflowImpl implements ReversalWorkflow {

    private final PostingEngine postingEngine;
    private final BoundedFinancialTransactionRetryExecutor retryExecutor;

    @Override
    public ReversalReceipt reverse(UUID trustedActorUserId, UUID originalJournalEntryId, String reason) {
        UUID actorId = Objects.requireNonNull(trustedActorUserId, "Trusted actor user ID must not be null");
        UUID originalId = Objects.requireNonNull(originalJournalEntryId, "Original journal entry ID must not be null");

        ReversalPostingResult result = retryExecutor.execute("REVERSAL", () -> postingEngine.postReversal(
                new ReversalCommand(originalId, PostingActor.user(actorId), LedgerChannel.WEB, reason)));
        PostingResult posted = result.getPostingResult();
        return new ReversalReceipt(
                posted.getEntryType(),
                posted.getEntryId(),
                posted.getEntryReference(),
                originalId,
                posted.getTotalAmount(),
                posted.getCurrency().getCode(),
                posted.getResultingCustomerBalances(),
                posted.getPostedAt(),
                result.isReplayed());
    }
}
