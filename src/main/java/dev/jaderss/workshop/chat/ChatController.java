package dev.jaderss.workshop.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
class ChatController {

    private final ChatClient chatClient;

    ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat() {
        return chatClient.prompt()
                .user("Tell me an interesting fact about Java")
                .call()
                .content();
    }

    @GetMapping(value = "/stream")
    public Flux<String> stream() {
        return chatClient.prompt()
                .user("Tell me 10 places I should visit in Rio")
                .stream()
                .content();
    }

    @GetMapping(value = "/joke")
    public ChatResponse joke() {
        return chatClient.prompt()
                .user("Tell me a dad joke")
                .call()
                .chatResponse();
    }

}
