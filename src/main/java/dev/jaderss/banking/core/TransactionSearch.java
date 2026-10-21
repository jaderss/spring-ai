package dev.jaderss.banking.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TransactionSearch(
        AccountNumber account, Instant from, Instant toExclusive, String merchant, BigDecimal amount) {

    public TransactionSearch {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(toExclusive, "toExclusive");
        if (!from.isBefore(toExclusive)) {
            throw new IllegalArgumentException("search range must start before it ends");
        }
        merchant = merchant == null || merchant.isBlank() ? null : merchant.strip();
        amount = amount == null ? null : amount.abs();
    }

    public static TransactionSearch between(AccountNumber account, Instant from, Instant toExclusive) {
        return new TransactionSearch(account, from, toExclusive, null, null);
    }
}
