package dev.jaderss.banking.insights;

public enum Category {
    GROCERIES,
    DINING,
    UTILITIES,
    HOUSING,
    TRANSPORT,
    ENTERTAINMENT,
    INCOME,
    TRANSFER,
    OTHER;

    boolean isSpending() {
        return this != INCOME && this != TRANSFER;
    }
}
