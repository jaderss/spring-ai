package dev.jaderss.banking.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import dev.jaderss.banking.assistant.ScriptedChatModel;
import dev.jaderss.banking.core.AccountNumber;
import dev.jaderss.banking.core.BankingTestSupport;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.core.TransactionRepository;
import dev.jaderss.banking.core.TransactionSearch;
import dev.jaderss.banking.privacy.PiiVaults;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BankingTestSupport.class, SpendingAnalyst.class, PiiVaults.class, SpendingAnalystTest.ScriptedModel.class})
class SpendingAnalystTest {

    private static final CustomerId ADA = new CustomerId("CUST-0001");
    private static final AccountNumber ADA_CHECKING = new AccountNumber("ACME-CHK-0001");
    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);
    private static final Map<String, Category> CATEGORY_BY_DESCRIPTION = Map.of(
            "Opening balance", Category.OTHER,
            "Example Corp Payroll", Category.INCOME,
            "Example Coffee Roasters", Category.DINING,
            "Sample Grocery Mart", Category.GROCERIES,
            "Placeholder Power & Light", Category.UTILITIES,
            "Fictional Streaming Co.", Category.ENTERTAINMENT,
            "Demo Rent Management", Category.HOUSING);

    @Autowired
    private SpendingAnalyst analyst;

    @Autowired
    private ScriptedChatModel model;

    @Autowired
    private TransactionRepository transactions;

    @BeforeEach
    void resetModel() {
        model.reset();
    }

    @Test
    void totalsComeFromStoredAmountsNotFromTheModel() {
        model.thenReply(validAnswer());

        MonthlySpendingSummary summary = analyst.summarize(ADA, ADA_CHECKING, SEPTEMBER);

        assertThat(summary.spentByCategory())
                .containsExactly(
                        entry(Category.GROCERIES, new BigDecimal("150.50")),
                        entry(Category.DINING, new BigDecimal("4.75")),
                        entry(Category.UTILITIES, new BigDecimal("112.35")),
                        entry(Category.HOUSING, new BigDecimal("1850.00")),
                        entry(Category.ENTERTAINMENT, new BigDecimal("15.99")));
        assertThat(summary.totalSpent()).isEqualTo(new BigDecimal("2133.59"));
        assertThat(summary.totalIncome()).isEqualTo(new BigDecimal("6400.00"));
        assertThat(summary.headline()).isEqualTo("Rent was the largest expense.");
        assertThat(model.prompts()).hasSize(1);
    }

    @Test
    void malformedJsonIsRetriedWithTheProblem() {
        model.thenReply("Sure! Here is your summary: rent was high.").thenReply(validAnswer());

        assertThat(analyst.summarize(ADA, ADA_CHECKING, SEPTEMBER).totalSpent()).isEqualTo(new BigDecimal("2133.59"));
        assertThat(model.prompts().getLast().getUserMessage().getText())
                .contains("Your previous answer was rejected: it was not valid JSON");
    }

    @Test
    void answerThatDropsATransactionIsRetried() {
        String incomplete = validAnswer().replaceFirst("\\{\"transactionId\":\"[^\"]+\",\"category\":\"[A-Z]+\"},", "");
        model.thenReply(incomplete).thenReply(validAnswer());

        analyst.summarize(ADA, ADA_CHECKING, SEPTEMBER);

        assertThat(model.prompts().getLast().getUserMessage().getText())
                .contains("the transactionIds must match the listed transactions exactly");
    }

    @Test
    void givesUpAfterThreeInvalidAnswers() {
        model.thenReply("{}").thenReply("not json").thenReply("{\"categorized\":[],\"headline\":\"\"}");

        assertThatThrownBy(() -> analyst.summarize(ADA, ADA_CHECKING, SEPTEMBER))
                .isInstanceOf(ModelOutputRejectedException.class)
                .hasMessageContaining("3 attempts");
        assertThat(model.prompts()).hasSize(SpendingAnalyst.MAX_ATTEMPTS);
    }

    @Test
    void refusesAccountsOfOtherCustomers() {
        assertThatThrownBy(() -> analyst.summarize(new CustomerId("CUST-0002"), ADA_CHECKING, SEPTEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("no account ACME-CHK-0001");
        assertThat(model.prompts()).isEmpty();
    }

    private String validAnswer() {
        String categorized = transactions
                .search(TransactionSearch.between(
                        ADA_CHECKING, Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z")))
                .stream()
                .map(transaction -> "{\"transactionId\":\"%s\",\"category\":\"%s\"}"
                        .formatted(transaction.id(), CATEGORY_BY_DESCRIPTION.get(transaction.description())))
                .collect(Collectors.joining(","));
        return "{\"categorized\":[" + categorized + "],\"headline\":\"Rent was the largest expense.\"}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ScriptedModel {

        @Bean
        ScriptedChatModel scriptedChatModel() {
            return new ScriptedChatModel();
        }

        @Bean
        ChatClient.Builder chatClientBuilder(ScriptedChatModel model) {
            return ChatClient.builder(model);
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }
    }
}
