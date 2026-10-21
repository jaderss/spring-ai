package dev.jaderss.banking.core;

import static dev.jaderss.banking.core.TransferRejectedException.Reason.ACCOUNT_NOT_FOUND;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.CURRENCY_MISMATCH;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.INSUFFICIENT_FUNDS;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.NOT_POSITIVE;
import static dev.jaderss.banking.core.TransferRejectedException.Reason.SAME_ACCOUNT;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final TransferRepository transfers;
    private final Clock clock;

    public TransferService(
            AccountRepository accounts, TransactionRepository transactions, TransferRepository transfers, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.transfers = transfers;
        this.clock = clock;
    }

    @Transactional
    public Transfer transfer(AccountNumber from, AccountNumber to, Money amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new TransferRejectedException(NOT_POSITIVE, "transfer amount must be positive");
        }
        if (from.equals(to)) {
            throw new TransferRejectedException(SAME_ACCOUNT, "cannot transfer to the same account");
        }
        Map<AccountNumber, Account> locked = accounts.lockForUpdate(List.of(from, to)).stream()
                .collect(Collectors.toMap(Account::number, Function.identity()));
        Account source = existing(locked, from);
        Account target = existing(locked, to);
        if (!amount.hasSameCurrencyAs(source.balance()) || !amount.hasSameCurrencyAs(target.balance())) {
            throw new TransferRejectedException(CURRENCY_MISMATCH, "both accounts must hold the transfer currency");
        }
        Money remaining = source.balance().subtract(amount);
        if (remaining.isNegative()) {
            throw new TransferRejectedException(INSUFFICIENT_FUNDS, "insufficient funds in " + from.value());
        }

        Instant now = clock.instant();
        Transfer transfer = new Transfer(UUID.randomUUID(), from, to, amount, now);
        accounts.updateBalance(from, remaining);
        accounts.updateBalance(to, target.balance().add(amount));
        transfers.insert(transfer);
        transactions.insert(
                new Transaction(UUID.randomUUID(), from, now, "Transfer to " + to.value(), null, amount.negate()));
        transactions.insert(new Transaction(UUID.randomUUID(), to, now, "Transfer from " + from.value(), null, amount));
        return transfer;
    }

    private static Account existing(Map<AccountNumber, Account> locked, AccountNumber number) {
        Account account = locked.get(number);
        if (account == null) {
            throw new TransferRejectedException(ACCOUNT_NOT_FOUND, "no account " + number.value());
        }
        return account;
    }
}
