package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.TransferProposal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProposalView(
        UUID proposalId, String fromAccount, String toAccount, BigDecimal amount, String currency, Instant expiresAt) {

    public static ProposalView of(TransferProposal proposal) {
        return new ProposalView(
                proposal.id(),
                proposal.from().value(),
                proposal.to().value(),
                proposal.amount().amount(),
                proposal.amount().currency().getCurrencyCode(),
                proposal.expiresAt());
    }
}
