package dev.jaderss.banking.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BankingTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferConcurrencyTest {

    private static final AccountNumber SOURCE = new AccountNumber("TEST-CHK-0001");
    private static final AccountNumber TARGET = new AccountNumber("TEST-SAV-0001");

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void concurrentTransfersNeverOverdrawTheSourceAccount() throws Exception {
        openTestAccounts();
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            attempts.add(this::transferTen);
        }

        List<Boolean> outcomes = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Future<Boolean> outcome : executor.invokeAll(attempts)) {
                outcomes.add(outcome.get());
            }
        }

        assertThat(outcomes).filteredOn(Boolean::booleanValue).hasSize(10);
        assertThat(accounts.findByNumber(SOURCE).orElseThrow().balance()).isEqualTo(Money.of("0", "USD"));
        assertThat(accounts.findByNumber(TARGET).orElseThrow().balance()).isEqualTo(Money.of("100", "USD"));
    }

    private boolean transferTen() {
        try {
            transferService.transfer(SOURCE, TARGET, Money.of("10", "USD"));
            return true;
        } catch (TransferRejectedException e) {
            return false;
        }
    }

    private void openTestAccounts() {
        jdbc.sql("insert into customer (id, full_name) values ('CUST-9001', 'Concurrency Test')")
                .update();
        jdbc.sql("""
                        insert into account (number, customer_id, type, currency, balance, opened_at) values
                            ('TEST-CHK-0001', 'CUST-9001', 'CHECKING', 'USD', 100.00, now()),
                            ('TEST-SAV-0001', 'CUST-9001', 'SAVINGS', 'USD', 0.00, now())
                        """).update();
    }
}
