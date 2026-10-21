package dev.jaderss.workshop.banking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

    private final JdbcClient jdbc;

    public TransactionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Transaction> findByAccount(AccountNumber account, Instant from, Instant toExclusive) {
        return jdbc.sql("""
                        select id, account_number, posted_at, description, memo, amount, currency
                        from account_transaction
                        where account_number = :account and posted_at >= :from and posted_at < :to
                        order by posted_at desc
                        """)
                .param("account", account.value())
                .param("from", utc(from))
                .param("to", utc(toExclusive))
                .query(TransactionRepository::transaction)
                .list();
    }

    void insert(Transaction transaction) {
        jdbc.sql("""
                        insert into account_transaction
                            (id, account_number, posted_at, description, memo, amount, currency)
                        values (:id, :account, :postedAt, :description, :memo, :amount, :currency)
                        """)
                .param("id", transaction.id())
                .param("account", transaction.account().value())
                .param("postedAt", utc(transaction.postedAt()))
                .param("description", transaction.description())
                .param("memo", transaction.memo())
                .param("amount", transaction.amount().amount())
                .param("currency", transaction.amount().currency().getCurrencyCode())
                .update();
    }

    static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Transaction transaction(ResultSet row, int rowNumber) throws SQLException {
        return new Transaction(
                row.getObject("id", UUID.class),
                new AccountNumber(row.getString("account_number")),
                row.getObject("posted_at", OffsetDateTime.class).toInstant(),
                row.getString("description"),
                row.getString("memo"),
                new Money(row.getBigDecimal("amount"), Currency.getInstance(row.getString("currency"))));
    }
}
