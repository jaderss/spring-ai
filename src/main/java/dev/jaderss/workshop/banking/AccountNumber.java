package dev.jaderss.workshop.banking;

import java.util.Objects;
import java.util.regex.Pattern;

public record AccountNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{4}-[A-Z]{3}-\\d{4}");

    public AccountNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("account number must look like ACME-CHK-0001");
        }
    }
}
