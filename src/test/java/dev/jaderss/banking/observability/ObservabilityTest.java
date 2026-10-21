package dev.jaderss.banking.observability;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jaderss.banking.BankingAssistantTest;
import dev.jaderss.banking.assistant.ScriptedChatModel;
import dev.jaderss.banking.web.AssistantController.AssistantReply;
import dev.jaderss.banking.web.AssistantController.AssistantRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@BankingAssistantTest
class ObservabilityTest {

    private final RestClient client;

    @Autowired
    private ScriptedChatModel model;

    @Autowired
    private RecordingObservationHandler observations;

    @Autowired
    private MeterRegistry meters;

    ObservabilityTest(@LocalServerPort int port) {
        client = RestClient.create("http://localhost:" + port);
    }

    @AfterEach
    void resetModel() {
        model.reset();
    }

    @Test
    void correlationIdFollowsTheRequestThroughModelAndToolObservations() {
        model.thenCallTool("getAccounts", "{}").thenReply("You have three accounts.");

        ResponseEntity<AssistantReply> response = chat("trace-0001");

        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo("trace-0001");
        assertThat(observationNamesCarrying("trace-0001")).contains("spring.ai.chat.client", "spring.ai.tool");
    }

    @Test
    void reportsTokenUsageSummedOverEveryModelCallOfTheRequest() {
        model.thenCallTool("getAccounts", "{}").thenReply("You have three accounts.");
        double inputBefore = recordedTokens("input");

        AssistantReply reply = chat("trace-0002").getBody();

        int calls = 2;
        assertThat(reply.usage().input()).isEqualTo(calls * ScriptedChatModel.INPUT_TOKENS_PER_CALL);
        assertThat(reply.usage().output()).isEqualTo(calls * ScriptedChatModel.OUTPUT_TOKENS_PER_CALL);
        assertThat(reply.usage().total())
                .isEqualTo(reply.usage().input() + reply.usage().output());
        assertThat(recordedTokens("input") - inputBefore)
                .isEqualTo(reply.usage().input());
    }

    @Test
    void invalidCorrelationIdIsReplaced() {
        model.thenReply("Hello.");

        String correlationId = chat("bad id; drop table").getHeaders().getFirst(CorrelationIdFilter.HEADER);

        assertThat(correlationId).matches("[0-9a-f-]{36}");
    }

    private ResponseEntity<AssistantReply> chat(String correlationId) {
        return client.post()
                .uri("/assistant/chat")
                .header("X-Customer-Id", "CUST-0001")
                .header(CorrelationIdFilter.HEADER, correlationId)
                .body(new AssistantRequest("observability", "Which accounts do I have?"))
                .retrieve()
                .toEntity(AssistantReply.class);
    }

    private List<String> observationNamesCarrying(String correlationId) {
        return observations.stopped().stream()
                .filter(context -> {
                    var keyValue = context.getHighCardinalityKeyValue(ObservabilityConfiguration.CORRELATION_KEY);
                    return keyValue != null && keyValue.getValue().equals(correlationId);
                })
                .map(Observation.Context::getName)
                .toList();
    }

    private double recordedTokens(String type) {
        return meters.get("banking.assistant.tokens")
                .tag("type", type)
                .summary()
                .totalAmount();
    }
}
