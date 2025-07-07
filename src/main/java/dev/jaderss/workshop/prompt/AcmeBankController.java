package dev.jaderss.workshop.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/acme")
@RequiredArgsConstructor
class AcmeBankController {

    private static final String ACME_BANK_PROMPT = """
        Here is a small set of instructions for a prompt to ensure it only answers questions about banking operations.

        Instructions for the AI:
        You are a specialized banking assistant. Your only function is to answer questions directly related to standard, everyday banking operations.

        Allowed Topics:
        - Account balance inquiries
        - Transaction history and details
        - Fund transfers and payments
        - Reporting lost or stolen cards
        - Information on bank statements

        Strictly Forbidden Topics:
        - Do not provide financial advice, investment recommendations, or opinions on markets.
        - Do not process loan applications or credit card applications.
        - Do not answer questions about insurance, wealth management, or complex financial products.
        - Do not engage in general conversation or answer questions outside of the allowed topics.

        If a question is outside your scope, you must politely decline and state that you can only assist with standard banking operations.
        """;

    private final ChatClient chatClient;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .system(ACME_BANK_PROMPT)
                .call()
                .content();
    }

}
