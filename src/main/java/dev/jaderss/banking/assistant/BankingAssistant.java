package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.knowledge.KnowledgeBaseLoader;
import dev.jaderss.banking.privacy.PiiAdvisor;
import dev.jaderss.banking.privacy.PiiVault;
import dev.jaderss.banking.privacy.PiiVaults;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BankingAssistant {

    private static final Logger log = LoggerFactory.getLogger(BankingAssistant.class);

    private final ChatClient chatClient;
    private final PiiVaults vaults;
    private final DistributionSummary inputTokens;
    private final DistributionSummary outputTokens;
    private final Clock clock;

    public BankingAssistant(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            BankingTools tools,
            VectorStore vectorStore,
            PiiVaults vaults,
            MeterRegistry meters,
            Clock clock,
            @Value("classpath:prompts/assistant-system.st") Resource systemPrompt) {
        this.chatClient = builder.defaultSystem(systemPrompt)
                .defaultTools(tools)
                .defaultAdvisors(
                        new PiiAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(4)
                                        .similarityThreshold(0.3)
                                        .build())
                                .build())
                .build();
        this.vaults = vaults;
        this.inputTokens = tokens(meters, "input");
        this.outputTokens = tokens(meters, "output");
        this.clock = clock;
    }

    public Answer reply(CustomerId customer, String conversationId, String message) {
        String conversation = customer.value() + "/" + conversationId;
        PiiVault vault = vaults.forConversation(conversation);
        ChatClientResponse response = chatClient
                .prompt()
                .system(system -> system.param("today", LocalDate.now(clock).toString()))
                .user(message)
                .toolContext(Map.of(BankingTools.CUSTOMER, customer, BankingTools.VAULT, vault))
                .advisors(advisor ->
                        advisor.param(ChatMemory.CONVERSATION_ID, conversation).param(PiiAdvisor.VAULT, vault))
                .call()
                .chatClientResponse();
        TokenUsage usage = TokenUsage.of(response.chatResponse().getMetadata().getUsage());
        inputTokens.record(usage.input());
        outputTokens.record(usage.output());
        log.info("Assistant replied using {} input and {} output tokens", usage.input(), usage.output());
        return new Answer(response.chatResponse().getResult().getOutput().getText(), sources(response), usage);
    }

    private static DistributionSummary tokens(MeterRegistry meters, String type) {
        return DistributionSummary.builder("banking.assistant.tokens")
                .description("Model tokens used to answer one assistant request, across all tool calls")
                .tag("type", type)
                .register(meters);
    }

    private static List<String> sources(ChatClientResponse response) {
        if (!(response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS) instanceof List<?> documents)) {
            return List.of();
        }
        return documents.stream()
                .map(Document.class::cast)
                .map(document -> String.valueOf(document.getMetadata().get(KnowledgeBaseLoader.SOURCE)))
                .distinct()
                .toList();
    }

    public record Answer(String text, List<String> sources, TokenUsage usage) {}

    public record TokenUsage(int input, int output, int total) {

        static TokenUsage of(Usage usage) {
            if (usage == null) {
                return new TokenUsage(0, 0, 0);
            }
            int input = Objects.requireNonNullElse(usage.getPromptTokens(), 0);
            int output = Objects.requireNonNullElse(usage.getCompletionTokens(), 0);
            return new TokenUsage(input, output, input + output);
        }
    }
}
