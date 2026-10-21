package dev.jaderss.banking.insights;

import dev.jaderss.banking.core.Account;
import dev.jaderss.banking.core.AccountNumber;
import dev.jaderss.banking.core.AccountRepository;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.core.Money;
import dev.jaderss.banking.core.Transaction;
import dev.jaderss.banking.core.TransactionRepository;
import dev.jaderss.banking.core.TransactionSearch;
import dev.jaderss.banking.insights.SpendingAnalysis.CategorizedTransaction;
import dev.jaderss.banking.privacy.PiiVault;
import dev.jaderss.banking.privacy.PiiVaults;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SpendingAnalyst {

    static final int MAX_ATTEMPTS = 3;

    private final ChatClient chatClient;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final JsonMapper jsonMapper;
    private final PiiVaults vaults;
    private final BeanOutputConverter<SpendingAnalysis> converter = new BeanOutputConverter<>(SpendingAnalysis.class);

    public SpendingAnalyst(
            ChatClient.Builder builder,
            AccountRepository accounts,
            TransactionRepository transactions,
            JsonMapper jsonMapper,
            PiiVaults vaults,
            @Value("classpath:prompts/spending-analyst.st") Resource systemPrompt) {
        this.chatClient = builder.defaultSystem(systemPrompt).build();
        this.accounts = accounts;
        this.transactions = transactions;
        this.jsonMapper = jsonMapper;
        this.vaults = vaults;
    }

    public MonthlySpendingSummary summarize(CustomerId customer, AccountNumber number, YearMonth month) {
        Account account = accounts.findByNumber(number)
                .filter(found -> found.owner().equals(customer))
                .orElseThrow(() -> new IllegalArgumentException("no account " + number.value()));
        List<Transaction> monthTransactions = transactions.search(TransactionSearch.between(
                number,
                month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        SpendingAnalysis analysis = analyze(monthTransactions);
        return summary(month, account, monthTransactions, analysis);
    }

    private SpendingAnalysis analyze(List<Transaction> monthTransactions) {
        PiiVault vault = vaults.newVault();
        String listing = jsonMapper.writeValueAsString(monthTransactions.stream()
                .map(transaction -> new TransactionLine(
                        transaction.id(),
                        transaction.postedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
                        vault.tokenize(transaction.description()),
                        transaction.amount().amount()))
                .toList());
        Set<UUID> expected = monthTransactions.stream().map(Transaction::id).collect(Collectors.toSet());
        String problem = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String request = "Transactions as JSON:\n" + listing + "\n\n" + converter.getFormat()
                    + (problem == null ? "" : "\n\nYour previous answer was rejected: " + problem + ". Answer again.");
            String content = chatClient.prompt().user(request).call().content();
            try {
                SpendingAnalysis analysis = converter.convert(content);
                problem = validate(analysis, expected);
                if (problem == null) {
                    return new SpendingAnalysis(analysis.categorized(), vault.reveal(analysis.headline()));
                }
            } catch (RuntimeException e) {
                problem = "it was not valid JSON for the requested schema";
            }
        }
        throw new ModelOutputRejectedException(MAX_ATTEMPTS, problem);
    }

    static String validate(SpendingAnalysis analysis, Set<UUID> expected) {
        if (analysis == null || analysis.categorized() == null) {
            return "categorized is missing";
        }
        if (analysis.headline() == null
                || analysis.headline().isBlank()
                || analysis.headline().length() > 200) {
            return "headline must hold 1 to 200 characters";
        }
        List<UUID> ids = analysis.categorized().stream()
                .map(CategorizedTransaction::transactionId)
                .toList();
        if (analysis.categorized().stream().anyMatch(item -> item.transactionId() == null || item.category() == null)) {
            return "every entry needs a transactionId and a category";
        }
        if (ids.size() != Set.copyOf(ids).size()) {
            return "a transaction appears more than once";
        }
        if (!Set.copyOf(ids).equals(expected)) {
            return "the transactionIds must match the listed transactions exactly";
        }
        return null;
    }

    private static MonthlySpendingSummary summary(
            YearMonth month, Account account, List<Transaction> monthTransactions, SpendingAnalysis analysis) {
        Map<UUID, Category> categories = analysis.categorized().stream()
                .collect(Collectors.toMap(CategorizedTransaction::transactionId, CategorizedTransaction::category));
        Money zero = new Money(BigDecimal.ZERO, account.balance().currency());
        Map<Category, Money> spent = new EnumMap<>(Category.class);
        Money income = zero;
        for (Transaction transaction : monthTransactions) {
            Category category = categories.get(transaction.id());
            if (category == Category.INCOME && transaction.amount().isPositive()) {
                income = income.add(transaction.amount());
            } else if (category.isSpending() && transaction.amount().isNegative()) {
                spent.merge(category, transaction.amount().negate(), Money::add);
            }
        }
        Money totalSpent = spent.values().stream().reduce(zero, Money::add);
        return new MonthlySpendingSummary(
                month,
                account.balance().currency().getCurrencyCode(),
                spent.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().amount(),
                                (left, right) -> left,
                                () -> new EnumMap<>(Category.class))),
                totalSpent.amount(),
                income.amount(),
                analysis.headline());
    }

    record TransactionLine(UUID transactionId, LocalDate date, String description, BigDecimal amount) {}
}
