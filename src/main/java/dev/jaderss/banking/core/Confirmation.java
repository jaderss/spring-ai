package dev.jaderss.banking.core;

public record Confirmation(Transfer transfer, boolean replayed) {}
