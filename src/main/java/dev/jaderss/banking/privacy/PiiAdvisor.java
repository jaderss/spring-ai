package dev.jaderss.banking.privacy;

import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;

public class PiiAdvisor implements CallAdvisor {

    public static final String VAULT = "pii.vault";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        PiiVault vault = Objects.requireNonNull((PiiVault) request.context().get(VAULT), VAULT);
        ChatClientRequest tokenized = request.mutate()
                .prompt(request.prompt()
                        .augmentUserMessage(user -> user.mutate()
                                .text(vault.tokenize(user.getText()))
                                .build()))
                .build();
        ChatClientResponse response = chain.nextCall(tokenized);
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return response;
        }
        return response.mutate()
                .chatResponse(ChatResponse.builder()
                        .from(chatResponse)
                        .generations(chatResponse.getResults().stream()
                                .map(generation -> new Generation(
                                        generation
                                                .getOutput()
                                                .mutate()
                                                .content(vault.reveal(
                                                        generation.getOutput().getText()))
                                                .build(),
                                        generation.getMetadata()))
                                .toList())
                        .build())
                .build();
    }

    @Override
    public String getName() {
        return "PiiAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
