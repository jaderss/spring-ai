package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.knowledge.KnowledgeBaseLoader;
import dev.jaderss.banking.privacy.PiiAdvisor;
import dev.jaderss.banking.privacy.PiiVault;
import dev.jaderss.banking.privacy.PiiVaults;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BankingAssistant {

    private final ChatClient chatClient;
    private final PiiVaults vaults;
    private final Clock clock;

    public BankingAssistant(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            BankingTools tools,
            VectorStore vectorStore,
            PiiVaults vaults,
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
        return new Answer(response.chatResponse().getResult().getOutput().getText(), sources(response));
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

    public record Answer(String text, List<String> sources) {}
}
