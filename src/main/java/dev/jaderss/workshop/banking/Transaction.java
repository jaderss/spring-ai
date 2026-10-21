package dev.jaderss.workshop.banking;

import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID id, AccountNumber account, Instant postedAt, String description, String memo, Money amount) {}
