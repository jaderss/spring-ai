package dev.jaderss.workshop.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ArticleController {

    private final ChatClient chatClient;

    private static final String ARTICLE_PROMPT = """
            Length & purpose: generate 500-word article to inform and engage audiences.
            Write in an informative yet conversational tone, suitable for a blog post.
            """;

    @GetMapping("/posts/new")
    public String newPost(@RequestParam(defaultValue = "JDK Virtual Threads") String topic) {
        return chatClient.prompt()
                .user(u -> {
                            u.text("Write a blog post about {topic}");
                            u.param("topic", topic);
                        }
                )
                .system(ARTICLE_PROMPT)
                .call()
                .content();
    }

}
