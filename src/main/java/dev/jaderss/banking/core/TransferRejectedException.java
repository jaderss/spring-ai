package dev.jaderss.banking.core;

public class TransferRejectedException extends RuntimeException {

    public enum Reason {
        ACCOUNT_NOT_FOUND,
        SAME_ACCOUNT,
        NOT_POSITIVE,
        CURRENCY_MISMATCH,
        INSUFFICIENT_FUNDS
    }

    private final Reason reason;

    public TransferRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
