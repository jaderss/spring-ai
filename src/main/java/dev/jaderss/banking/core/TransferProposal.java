package dev.jaderss.banking.core;

import java.time.Instant;
import java.util.UUID;

public record TransferProposal(
        UUID id, CustomerId customer, AccountNumber from, AccountNumber to, Money amount, Instant expiresAt) {}
