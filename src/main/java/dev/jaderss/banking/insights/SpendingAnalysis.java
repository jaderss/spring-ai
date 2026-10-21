package dev.jaderss.banking.insights;

import java.util.List;
import java.util.UUID;

public record SpendingAnalysis(List<CategorizedTransaction> categorized, String headline) {

    public record CategorizedTransaction(UUID transactionId, Category category) {}
}
