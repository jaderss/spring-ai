package dev.jaderss.banking.assistant;

import dev.jaderss.banking.TestcontainersConfiguration;
import dev.jaderss.banking.observability.RecordingObservationHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
@Import(TestcontainersConfiguration.class)
public class ScriptedAssistantConfiguration {

    public static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Bean
    ScriptedChatModel scriptedChatModel() {
        return new ScriptedChatModel();
    }

    @Bean
    RecordingObservationHandler recordingObservationHandler() {
        return new RecordingObservationHandler();
    }

    @Bean
    FakeEmbeddingModel fakeEmbeddingModel() {
        return new FakeEmbeddingModel();
    }

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
