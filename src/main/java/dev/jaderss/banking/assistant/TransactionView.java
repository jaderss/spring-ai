package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.Transaction;
import dev.jaderss.banking.privacy.PiiVault;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(Instant postedAt, String description, String memo, BigDecimal amount, String currency) {

    static TransactionView of(Transaction transaction, PiiVault vault) {
        return new TransactionView(
                transaction.postedAt(),
                vault.tokenize(transaction.description()),
                vault.tokenize(transaction.memo()),
                transaction.amount().amount(),
                transaction.amount().currency().getCurrencyCode());
    }
}
