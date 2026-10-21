package dev.jaderss.banking.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TransferRepository {

    private final JdbcClient jdbc;

    public TransferRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void insert(Transfer transfer) {
        jdbc.sql("""
                        insert into transfer (id, from_account, to_account, amount, currency, created_at)
                        values (:id, :from, :to, :amount, :currency, :createdAt)
                        """)
                .param("id", transfer.id())
                .param("from", transfer.from().value())
                .param("to", transfer.to().value())
                .param("amount", transfer.amount().amount())
                .param("currency", transfer.amount().currency().getCurrencyCode())
                .param("createdAt", TransactionRepository.utc(transfer.createdAt()))
                .update();
    }

    Optional<Transfer> findById(UUID id) {
        return jdbc.sql(
                        "select id, from_account, to_account, amount, currency, created_at from transfer where id = :id")
                .param("id", id)
                .query(TransferRepository::transfer)
                .optional();
    }

    private static Transfer transfer(ResultSet row, int rowNumber) throws SQLException {
        return new Transfer(
                row.getObject("id", UUID.class),
                new AccountNumber(row.getString("from_account")),
                new AccountNumber(row.getString("to_account")),
                new Money(row.getBigDecimal("amount"), Currency.getInstance(row.getString("currency"))),
                row.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
