package dev.jaderss.workshop.tools.datetime;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DateTimeController {

    private final ChatClient chatClient;

    @GetMapping("/tools")
    public String tools() {
        return chatClient.prompt()
                .user("What is tomorrow's date?")
                .tools(new DateTimeTools())
                .call()
                .content();
    }
}
