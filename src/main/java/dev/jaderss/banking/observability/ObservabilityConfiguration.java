package dev.jaderss.banking.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ObservabilityConfiguration {

    static final String CORRELATION_KEY = "banking.correlation.id";

    @Bean
    ObservationFilter correlationIdObservationFilter() {
        return context -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null) {
                context.addHighCardinalityKeyValue(KeyValue.of(CORRELATION_KEY, correlationId));
            }
            return context;
        };
    }
}
