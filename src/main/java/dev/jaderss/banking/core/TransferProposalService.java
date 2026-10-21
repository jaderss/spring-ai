package dev.jaderss.banking.core;

import static dev.jaderss.banking.core.TransferRejectedException.Reason.ACCOUNT_NOT_FOUND;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.ALREADY_CONFIRMED;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.CURRENCY_MISMATCH;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.IDEMPOTENCY_KEY_REUSED;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.NOT_POSITIVE;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.PROPOSAL_EXPIRED;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.PROPOSAL_NOT_FOUND;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.SAME_ACCOUNT;

import dev.jaderss.banking.core.TransferProposalRepository.StoredProposal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferProposalService {

    static final Duration PROPOSAL_LIFETIME = Duration.ofMinutes(10);
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    private final AccountRepository accounts;
    private final TransferProposalRepository proposals;
    private final TransferRepository transfers;
    private final TransferService transferService;
    private final Clock clock;

    public TransferProposalService(
            AccountRepository accounts,
            TransferProposalRepository proposals,
            TransferRepository transfers,
            TransferService transferService,
            Clock clock) {
        this.accounts = accounts;
        this.proposals = proposals;
        this.transfers = transfers;
        this.transferService = transferService;
        this.clock = clock;
    }

    public TransferProposal propose(CustomerId customer, AccountNumber from, AccountNumber to, Money amount) {
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new TransferRejectedException(NOT_POSITIVE, "transfer amount must be positive");
        }
        if (from.equals(to)) {
            throw new TransferRejectedException(SAME_ACCOUNT, "cannot transfer to the same account");
        }
        Account source = accounts.findByNumber(from)
                .filter(account -> account.owner().equals(customer))
                .orElseThrow(() -> new TransferRejectedException(ACCOUNT_NOT_FOUND, "no account " + from.value()));
        Account target = accounts.findByNumber(to)
                .orElseThrow(() -> new TransferRejectedException(ACCOUNT_NOT_FOUND, "no account " + to.value()));
        if (!amount.hasSameCurrencyAs(source.balance()) || !amount.hasSameCurrencyAs(target.balance())) {
            throw new TransferRejectedException(CURRENCY_MISMATCH, "both accounts must hold the transfer currency");
        }
        TransferProposal proposal = new TransferProposal(
                UUID.randomUUID(), customer, from, to, amount, clock.instant().plus(PROPOSAL_LIFETIME));
        proposals.insert(proposal);
        return proposal;
    }

    public List<TransferProposal> pending(CustomerId customer) {
        return proposals.findPending(customer, clock.instant());
    }

    @Transactional
    public Confirmation confirm(CustomerId customer, UUID proposalId, String idempotencyKey) {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (!IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new IllegalArgumentException("Idempotency-Key must be 8 to 64 letters, digits, - or _");
        }
        StoredProposal stored = proposals
                .lockForConfirmation(proposalId, customer)
                .orElseThrow(() -> new TransferRejectedException(PROPOSAL_NOT_FOUND, "no proposal " + proposalId));
        if (stored.confirmed()) {
            if (!stored.idempotencyKey().equals(idempotencyKey)) {
                throw new TransferRejectedException(ALREADY_CONFIRMED, "proposal was already confirmed");
            }
            return new Confirmation(transfers.findById(stored.transferId()).orElseThrow(), true);
        }
        TransferProposal proposal = stored.proposal();
        Instant now = clock.instant();
        if (!now.isBefore(proposal.expiresAt())) {
            throw new TransferRejectedException(PROPOSAL_EXPIRED, "proposal expired, ask for a new one");
        }
        Transfer transfer = transferService.transfer(proposal.from(), proposal.to(), proposal.amount());
        try {
            proposals.markConfirmed(proposal.id(), transfer.id(), idempotencyKey);
        } catch (DuplicateKeyException e) {
            throw new TransferRejectedException(
                    IDEMPOTENCY_KEY_REUSED, "Idempotency-Key was already used for another proposal");
        }
        return new Confirmation(transfer, false);
    }
}
