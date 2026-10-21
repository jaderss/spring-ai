package dev.jaderss.banking.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TransferProposalRepository {

    private static final String SELECT_PROPOSAL = """
            select id, customer_id, from_account, to_account, amount, currency, expires_at, transfer_id, idempotency_key
            from transfer_proposal
            """;

    private final JdbcClient jdbc;

    public TransferProposalRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TransferProposal> findPending(CustomerId customer, Instant now) {
        return jdbc
                .sql(SELECT_PROPOSAL
                        + " where customer_id = :customer and transfer_id is null and expires_at > :now"
                        + " order by expires_at")
                .param("customer", customer.value())
                .param("now", TransactionRepository.utc(now))
                .query(TransferProposalRepository::stored)
                .list()
                .stream()
                .map(StoredProposal::proposal)
                .toList();
    }

    void insert(TransferProposal proposal) {
        jdbc.sql("""
                        insert into transfer_proposal
                            (id, customer_id, from_account, to_account, amount, currency, expires_at)
                        values (:id, :customer, :from, :to, :amount, :currency, :expiresAt)
                        """)
                .param("id", proposal.id())
                .param("customer", proposal.customer().value())
                .param("from", proposal.from().value())
                .param("to", proposal.to().value())
                .param("amount", proposal.amount().amount())
                .param("currency", proposal.amount().currency().getCurrencyCode())
                .param("expiresAt", TransactionRepository.utc(proposal.expiresAt()))
                .update();
    }

    Optional<StoredProposal> lockForConfirmation(UUID id, CustomerId customer) {
        return jdbc.sql(SELECT_PROPOSAL + " where id = :id and customer_id = :customer for update")
                .param("id", id)
                .param("customer", customer.value())
                .query(TransferProposalRepository::stored)
                .optional();
    }

    void markConfirmed(UUID id, UUID transferId, String idempotencyKey) {
        jdbc.sql("update transfer_proposal set transfer_id = :transfer, idempotency_key = :key where id = :id")
                .param("transfer", transferId)
                .param("key", idempotencyKey)
                .param("id", id)
                .update();
    }

    record StoredProposal(TransferProposal proposal, UUID transferId, String idempotencyKey) {

        boolean confirmed() {
            return transferId != null;
        }
    }

    private static StoredProposal stored(ResultSet row, int rowNumber) throws SQLException {
        TransferProposal proposal = new TransferProposal(
                row.getObject("id", UUID.class),
                new CustomerId(row.getString("customer_id")),
                new AccountNumber(row.getString("from_account")),
                new AccountNumber(row.getString("to_account")),
                new Money(row.getBigDecimal("amount"), Currency.getInstance(row.getString("currency"))),
                row.getObject("expires_at", OffsetDateTime.class).toInstant());
        return new StoredProposal(proposal, row.getObject("transfer_id", UUID.class), row.getString("idempotency_key"));
    }
}
