package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.CustomerId;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BankingAssistant {

    private final ChatClient chatClient;
    private final Clock clock;

    public BankingAssistant(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            BankingTools tools,
            Clock clock,
            @Value("classpath:prompts/assistant-system.st") Resource systemPrompt) {
        this.chatClient = builder.defaultSystem(systemPrompt)
                .defaultTools(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.clock = clock;
    }

    public String reply(CustomerId customer, String conversationId, String message) {
        return chatClient
                .prompt()
                .system(system -> system.param("today", LocalDate.now(clock).toString()))
                .user(message)
                .toolContext(Map.of(BankingTools.CUSTOMER, customer))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, customer.value() + "/" + conversationId))
                .call()
                .content();
    }
}
