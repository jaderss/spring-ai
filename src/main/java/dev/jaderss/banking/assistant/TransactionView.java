package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.Transaction;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(Instant postedAt, String description, String memo, BigDecimal amount, String currency) {

    static TransactionView of(Transaction transaction) {
        return new TransactionView(
                transaction.postedAt(),
                transaction.description(),
                transaction.memo(),
                transaction.amount().amount(),
                transaction.amount().currency().getCurrencyCode());
    }
}
