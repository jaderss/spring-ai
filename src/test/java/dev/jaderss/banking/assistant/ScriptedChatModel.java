package dev.jaderss.banking.assistant;

import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

public class ScriptedChatModel implements ChatModel {

    private final Deque<AssistantMessage> script = new ConcurrentLinkedDeque<>();
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

    public ScriptedChatModel thenCallTool(String toolName, String argumentsJson) {
        script.add(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        UUID.randomUUID().toString(), "function", toolName, argumentsJson)))
                .build());
        return this;
    }

    public ScriptedChatModel thenReply(String text) {
        script.add(AssistantMessage.builder().content(text).build());
        return this;
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public void reset() {
        script.clear();
        prompts.clear();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        AssistantMessage next = script.poll();
        if (next == null) {
            throw new IllegalStateException("the scripted conversation has no reply left");
        }
        return new ChatResponse(List.of(new Generation(next)));
    }

    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }
}
