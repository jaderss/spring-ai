package dev.jaderss.workshop.banking;

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
}
