package dev.jaderss.banking.web;

import dev.jaderss.banking.assistant.ProposalView;
import dev.jaderss.banking.core.Confirmation;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.core.Transfer;
import dev.jaderss.banking.core.TransferProposalService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers/proposals")
public class TransferController {

    private final TransferProposalService proposals;

    public TransferController(TransferProposalService proposals) {
        this.proposals = proposals;
    }

    @GetMapping
    public List<ProposalView> pending(@RequestHeader(Headers.CUSTOMER) String customerId) {
        return proposals.pending(new CustomerId(customerId)).stream()
                .map(ProposalView::of)
                .toList();
    }

    @PostMapping("/{proposalId}/confirm")
    public ResponseEntity<TransferView> confirm(
            @RequestHeader(Headers.CUSTOMER) String customerId,
            @RequestHeader(Headers.IDEMPOTENCY_KEY) String idempotencyKey,
            @PathVariable UUID proposalId) {
        Confirmation confirmation = proposals.confirm(new CustomerId(customerId), proposalId, idempotencyKey);
        return ResponseEntity.status(confirmation.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(TransferView.of(confirmation.transfer()));
    }

    public record TransferView(
            UUID transferId,
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String currency,
            Instant createdAt) {

        static TransferView of(Transfer transfer) {
            return new TransferView(
                    transfer.id(),
                    transfer.from().value(),
                    transfer.to().value(),
                    transfer.amount().amount(),
                    transfer.amount().currency().getCurrencyCode(),
                    transfer.createdAt());
        }
    }
}
