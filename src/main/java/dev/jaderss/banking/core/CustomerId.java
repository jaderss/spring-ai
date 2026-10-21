package dev.jaderss.banking.core;

import java.util.Objects;
import java.util.regex.Pattern;

public record CustomerId(String value) {

    private static final Pattern FORMAT = Pattern.compile("CUST-\\d{4}");

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("customer id must look like CUST-0001");
        }
    }
}
