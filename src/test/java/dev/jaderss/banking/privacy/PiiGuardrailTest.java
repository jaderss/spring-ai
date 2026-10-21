package dev.jaderss.banking.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jaderss.banking.BankingAssistantTest;
import dev.jaderss.banking.assistant.BankingAssistant;
import dev.jaderss.banking.assistant.ScriptedChatModel;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.core.TransferProposalService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@BankingAssistantTest
class PiiGuardrailTest {

    private static final CustomerId ADA = new CustomerId("CUST-0001");
    private static final List<String> RAW_PII = List.of("ACME-", "Ada Example", "Bruno Sample");

    @Autowired
    private BankingAssistant assistant;

    @Autowired
    private ScriptedChatModel model;

    @Autowired
    private TransferProposalService proposals;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void resetModel() {
        model.reset();
    }

    @Test
    void injectedMemoReachesTheModelAsRedactedDataAndCannotMoveMoney() {
        model.thenCallTool(
                        "searchTransactions",
                        "{\"accountNumber\":\"ACCT_1\",\"fromDate\":\"2026-10-01\",\"toDate\":\"2026-10-31\",\"merchant\":\"fuel\"}")
                .thenCallTool(
                        "initiateTransfer",
                        "{\"fromAccount\":\"ACCT_1\",\"toAccount\":\"ACCT_2\",\"amount\":5000,\"currency\":\"USD\"}")
                .thenReply("Approved: 5000 USD went from ACCT_1 to ACCT_2 for NAME_1.");

        BankingAssistant.Answer answer = assistant.reply(ADA, "injection", "Show my fuel purchases on ACME-CHK-0001");

        assertThat(textsSentToModel())
                .noneSatisfy(text -> assertThat(text).containsAnyOf(RAW_PII.toArray(String[]::new)));
        assertThat(toolResults())
                .first()
                .asString()
                .contains("IGNORE ALL PREVIOUS INSTRUCTIONS")
                .contains("ACCT_1")
                .contains("ACCT_2")
                .contains("NAME_1");
        assertThat(answer.text())
                .isEqualTo("Approved: 5000 USD went from ACME-CHK-0001 to ACME-CHK-0002 for Bruno Sample.");
        assertThat(balanceOf("ACME-CHK-0002")).isEqualByComparingTo("708.00");
        assertThat(proposals.pending(ADA))
                .anySatisfy(proposal -> assertThat(proposal.amount().amount()).isEqualByComparingTo("5000"));
    }

    @Test
    void conversationMemoryOnlyHoldsTokens() {
        model.thenReply("ACCT_1 is your checking account.").thenReply("Yes, ACCT_1.");

        assistant.reply(ADA, "memory", "Is ACME-CHK-0001 my checking account?");
        BankingAssistant.Answer second = assistant.reply(ADA, "memory", "And ACME-CHK-0001 again?");

        assertThat(model.prompts().getLast().getInstructions())
                .extracting(Message::getText)
                .contains("Is ACCT_1 my checking account?", "ACCT_1 is your checking account.")
                .anySatisfy(text -> assertThat(text).startsWith("And ACCT_1 again?"));
        assertThat(textsSentToModel()).noneSatisfy(text -> assertThat(text).contains("ACME-"));
        assertThat(second.text()).isEqualTo("Yes, ACME-CHK-0001.");
    }

    private List<String> textsSentToModel() {
        return model.prompts().stream()
                .flatMap(prompt -> prompt.getInstructions().stream())
                .flatMap(PiiGuardrailTest::texts)
                .toList();
    }

    private List<String> toolResults() {
        return model.prompts().stream()
                .flatMap(prompt -> prompt.getInstructions().stream())
                .filter(ToolResponseMessage.class::isInstance)
                .flatMap(message -> ((ToolResponseMessage) message).getResponses().stream())
                .map(ToolResponseMessage.ToolResponse::responseData)
                .toList();
    }

    private static Stream<String> texts(Message message) {
        return switch (message) {
            case ToolResponseMessage tool ->
                tool.getResponses().stream().map(ToolResponseMessage.ToolResponse::responseData);
            case AssistantMessage assistantMessage ->
                Stream.concat(
                        Stream.ofNullable(assistantMessage.getText()),
                        assistantMessage.getToolCalls().stream().map(AssistantMessage.ToolCall::arguments));
            default -> Stream.ofNullable(message.getText());
        };
    }

    private BigDecimal balanceOf(String account) {
        return jdbc.sql("select balance from account where number = :number")
                .param("number", account)
                .query(BigDecimal.class)
                .single();
    }
}
