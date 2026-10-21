package dev.jaderss.banking.web;

import dev.jaderss.banking.assistant.BankingAssistant;
import dev.jaderss.banking.assistant.ProposalView;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.core.TransferProposalService;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssistantController {

    private final BankingAssistant assistant;
    private final TransferProposalService proposals;

    public AssistantController(BankingAssistant assistant, TransferProposalService proposals) {
        this.assistant = assistant;
        this.proposals = proposals;
    }

    @PostMapping("/assistant/chat")
    public AssistantReply chat(
            @RequestHeader(Headers.CUSTOMER) String customerId, @RequestBody AssistantRequest request) {
        CustomerId customer = new CustomerId(customerId);
        BankingAssistant.Answer answer = assistant.reply(customer, request.conversationId(), request.message());
        return new AssistantReply(
                answer.text(),
                answer.sources(),
                proposals.pending(customer).stream().map(ProposalView::of).toList());
    }

    public record AssistantRequest(String conversationId, String message) {

        private static final Pattern CONVERSATION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

        public AssistantRequest {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(message, "message");
            if (!CONVERSATION_ID.matcher(conversationId).matches()) {
                throw new IllegalArgumentException("conversationId must be 1 to 64 letters, digits, - or _");
            }
            if (message.isBlank() || message.length() > 4000) {
                throw new IllegalArgumentException("message must hold 1 to 4000 characters");
            }
        }
    }

    public record AssistantReply(String reply, List<String> sources, List<ProposalView> pendingProposals) {}
}
