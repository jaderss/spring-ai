package dev.jaderss.banking.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jaderss.banking.assistant.ProposalView;
import dev.jaderss.banking.assistant.ScriptedAssistantConfiguration;
import dev.jaderss.banking.assistant.ScriptedChatModel;
import dev.jaderss.banking.web.AssistantController.AssistantReply;
import dev.jaderss.banking.web.AssistantController.AssistantRequest;
import dev.jaderss.banking.web.TransferController.TransferView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.ai.model.chat=none", "spring.ai.openai.api-key=test-key"})
@Import(ScriptedAssistantConfiguration.class)
class AssistantFlowTest {

    private final RestClient client;

    @Autowired
    private ScriptedChatModel model;

    @Autowired
    private JdbcClient jdbc;

    AssistantFlowTest(@LocalServerPort int port) {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    @AfterEach
    void resetModel() {
        model.reset();
    }

    @Test
    void assistantAnswersFromToolResultsWithTodaysDateInTheSystemPrompt() {
        model.thenCallTool("getBalance", "{\"accountNumber\":\"ACME-SAV-0001\"}")
                .thenReply("You have 10012.50 USD.");

        AssistantReply reply = chat("CUST-0001", "Balance of my savings?");

        assertThat(reply.reply()).isEqualTo("You have 10012.50 USD.");
        assertThat(model.prompts().getFirst().getSystemMessage().getText()).contains("Today is 2026-10-20");
        assertThat(lastToolResults()).singleElement().asString().contains("10012.50");
    }

    @Test
    void modelCanOnlyProposeAndTheConfirmationPostsExactlyOnce() {
        model.thenCallTool(
                        "initiateTransfer",
                        "{\"fromAccount\":\"ACME-CHK-0001\",\"toAccount\":\"ACME-CHK-0002\",\"amount\":25.00,\"currency\":\"USD\"}")
                .thenReply("I proposed sending 25.00 USD to ACME-CHK-0002. Please confirm it in the app.");

        AssistantReply reply = chat("CUST-0001", "Send 25 dollars to Bruno's checking");

        ProposalView proposal = reply.pendingProposals().getLast();
        assertThat(proposal.amount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(balanceOf("ACME-CHK-0002")).isEqualByComparingTo("708.00");

        ResponseEntity<TransferView> first = confirm("CUST-0001", proposal, "confirm-000001", TransferView.class);
        ResponseEntity<TransferView> replay = confirm("CUST-0001", proposal, "confirm-000001", TransferView.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().transferId()).isEqualTo(first.getBody().transferId());
        assertThat(balanceOf("ACME-CHK-0002")).isEqualByComparingTo("733.00");
    }

    @Test
    void anotherCustomerCannotConfirmTheProposal() {
        model.thenCallTool(
                        "initiateTransfer",
                        "{\"fromAccount\":\"ACME-CHK-0001\",\"toAccount\":\"ACME-SAV-0001\",\"amount\":5,\"currency\":\"USD\"}")
                .thenReply("Proposed.");
        ProposalView proposal = chat("CUST-0001", "Move 5 dollars to savings")
                .pendingProposals()
                .getLast();

        ResponseEntity<ProblemDetail> response = confirm("CUST-0002", proposal, "confirm-000002", ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getProperties()).containsEntry("reason", "PROPOSAL_NOT_FOUND");
    }

    @Test
    void toolsCannotReadAnotherCustomersAccount() {
        model.thenCallTool("getBalance", "{\"accountNumber\":\"ACME-CHK-0002\"}")
                .thenReply("I could not find it.");

        chat("CUST-0001", "What is the balance of ACME-CHK-0002?");

        assertThat(lastToolResults())
                .singleElement()
                .asString()
                .contains("no account ACME-CHK-0002")
                .doesNotContain("708");
    }

    private List<String> lastToolResults() {
        return model.prompts().getLast().getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .flatMap(message -> message.getResponses().stream())
                .map(ToolResponseMessage.ToolResponse::responseData)
                .toList();
    }

    private AssistantReply chat(String customer, String message) {
        return client.post()
                .uri("/assistant/chat")
                .header(Headers.CUSTOMER, customer)
                .body(new AssistantRequest("test", message))
                .retrieve()
                .body(AssistantReply.class);
    }

    private <T> ResponseEntity<T> confirm(String customer, ProposalView proposal, String key, Class<T> type) {
        return client.post()
                .uri("/transfers/proposals/{id}/confirm", proposal.proposalId())
                .header(Headers.CUSTOMER, customer)
                .header(Headers.IDEMPOTENCY_KEY, key)
                .retrieve()
                .toEntity(type);
    }

    private BigDecimal balanceOf(String account) {
        return jdbc.sql("select balance from account where number = :number")
                .param("number", account)
                .query(BigDecimal.class)
                .single();
    }
}
