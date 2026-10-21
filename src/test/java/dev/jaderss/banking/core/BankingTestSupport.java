package dev.jaderss.banking.core;

import dev.jaderss.banking.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({
    TestcontainersConfiguration.class,
    AccountRepository.class,
    TransactionRepository.class,
    TransferRepository.class,
    TransferService.class,
    TransferProposalRepository.class,
    TransferProposalService.class
})
public class BankingTestSupport {

    public static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Bean
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
