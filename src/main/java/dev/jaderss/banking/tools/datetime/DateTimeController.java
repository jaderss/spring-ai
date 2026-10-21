package dev.jaderss.banking.tools.datetime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DateTimeController {

    private final ChatClient chatClient;

    public DateTimeController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/tools")
    public String tools() {
        return chatClient
                .prompt()
                .user("What is tomorrow's date?")
                .tools(new DateTimeTools())
                .call()
                .content();
    }
}
