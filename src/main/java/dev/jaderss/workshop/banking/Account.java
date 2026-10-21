package dev.jaderss.workshop.banking;

import java.time.Instant;

public record Account(AccountNumber number, CustomerId owner, AccountType type, Money balance, Instant openedAt) {}
