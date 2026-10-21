package dev.jaderss.workshop.banking;

import java.time.Instant;
import java.util.UUID;

public record Transfer(UUID id, AccountNumber from, AccountNumber to, Money amount, Instant createdAt) {}
