package dev.jaderss.workshop.output;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
class VacationPlanController {

    public static final String VACATION_PLAN = """
            I have 5 days off and want to go on vacation to Rio de Janeiro, Brazil.  I want to visit the Christ the
            Redeemer statue, Sugarloaf Mountain, and Copacabana beach. I also want to try some local food and drink some
            caipirinhas. Can you help me plan my trip?
            List:
            - name of activity: example: "Visit Christ the Redeemer"
            - location: example: "Christ the Redeemer"
            - day: example: "Day 1"
            - time: example: "10:00 AM"
           * Escape all special characters in the output, such as quotes and newlines to fit a Java String.
           """;

    private final ChatClient chatClient;

    @GetMapping("/vacation/unstructured")
    public String unstructured() {
        return chatClient.prompt().user(VACATION_PLAN).call().content();
    }

    @GetMapping("/vacation/structured")
    public Itinerary structured() {
        return chatClient.prompt().user(VACATION_PLAN).call().entity(Itinerary.class);
    }

}
