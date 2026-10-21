package dev.jaderss.banking.core;

import java.time.Instant;
import java.util.UUID;

public record Transfer(UUID id, AccountNumber from, AccountNumber to, Money amount, Instant createdAt) {}
