package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.Account;
import java.math.BigDecimal;

public record AccountView(String number, String type, BigDecimal balance, String currency) {

    static AccountView of(Account account) {
        return new AccountView(
                account.number().value(),
                account.type().name(),
                account.balance().amount(),
                account.balance().currency().getCurrencyCode());
    }
}
