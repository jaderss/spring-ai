package dev.jaderss.workshop.banking;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BankingConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
