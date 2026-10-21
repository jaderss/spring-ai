package dev.jaderss.workshop.banking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private static final String SELECT_ACCOUNT =
            "select number, customer_id, type, currency, balance, opened_at from account";

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Account> findByOwner(CustomerId owner) {
        return jdbc.sql(SELECT_ACCOUNT + " where customer_id = :owner order by number")
                .param("owner", owner.value())
                .query(AccountRepository::account)
                .list();
    }

    public Optional<Account> findByNumber(AccountNumber number) {
        return jdbc.sql(SELECT_ACCOUNT + " where number = :number")
                .param("number", number.value())
                .query(AccountRepository::account)
                .optional();
    }

    List<Account> lockForUpdate(Collection<AccountNumber> numbers) {
        return jdbc.sql(SELECT_ACCOUNT + " where number in (:numbers) order by number for update")
                .param("numbers", numbers.stream().map(AccountNumber::value).toList())
                .query(AccountRepository::account)
                .list();
    }

    void updateBalance(AccountNumber number, Money balance) {
        jdbc.sql("update account set balance = :balance where number = :number")
                .param("balance", balance.amount())
                .param("number", number.value())
                .update();
    }

    private static Account account(ResultSet row, int rowNumber) throws SQLException {
        return new Account(
                new AccountNumber(row.getString("number")),
                new CustomerId(row.getString("customer_id")),
                AccountType.valueOf(row.getString("type")),
                new Money(row.getBigDecimal("balance"), Currency.getInstance(row.getString("currency"))),
                row.getObject("opened_at", OffsetDateTime.class).toInstant());
    }
}
