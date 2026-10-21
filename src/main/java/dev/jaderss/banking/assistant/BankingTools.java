package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.Account;
import dev.jaderss.banking.core.AccountNumber;
import dev.jaderss.banking.core.AccountRepository;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.core.Money;
import dev.jaderss.banking.core.TransactionRepository;
import dev.jaderss.banking.core.TransactionSearch;
import dev.jaderss.banking.core.TransferProposal;
import dev.jaderss.banking.core.TransferProposalService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BankingTools {

    static final String CUSTOMER = "customer";

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final TransferProposalService proposals;

    public BankingTools(
            AccountRepository accounts, TransactionRepository transactions, TransferProposalService proposals) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.proposals = proposals;
    }

    @Tool(description = "List the customer's accounts with their type, balance and currency")
    public List<AccountView> getAccounts(ToolContext context) {
        return accounts.findByOwner(customer(context)).stream()
                .map(AccountView::of)
                .toList();
    }

    @Tool(description = "Get the current balance of one of the customer's accounts")
    public AccountView getBalance(
            @ToolParam(description = "Account number, for example ACME-CHK-0001") String accountNumber,
            ToolContext context) {
        return AccountView.of(ownedAccount(accountNumber, context));
    }

    @Tool(description = "Search transactions of one of the customer's accounts, newest first, at most 100")
    public List<TransactionView> searchTransactions(
            @ToolParam(description = "Account number, for example ACME-CHK-0001") String accountNumber,
            @ToolParam(description = "First day to include, ISO date such as 2026-09-01") LocalDate fromDate,
            @ToolParam(description = "Last day to include, ISO date such as 2026-09-30") LocalDate toDate,
            @ToolParam(description = "Part of the merchant or description, case insensitive", required = false)
                    String merchant,
            @ToolParam(description = "Exact amount regardless of sign, such as 4.75", required = false)
                    BigDecimal amount,
            ToolContext context) {
        Account account = ownedAccount(accountNumber, context);
        TransactionSearch search = new TransactionSearch(
                account.number(), startOf(fromDate), startOf(toDate.plusDays(1)), merchant, amount);
        return transactions.search(search).stream().map(TransactionView::of).toList();
    }

    @Tool(
            description = "Propose a transfer from one of the customer's accounts. This moves no money: the customer"
                    + " must confirm the proposal in the app before it expires.")
    public ProposalView initiateTransfer(
            @ToolParam(description = "Source account number, owned by the customer") String fromAccount,
            @ToolParam(description = "Target account number") String toAccount,
            @ToolParam(description = "Positive amount such as 100.00") BigDecimal amount,
            @ToolParam(description = "ISO currency code such as USD") String currency,
            ToolContext context) {
        TransferProposal proposal = proposals.propose(
                customer(context),
                new AccountNumber(fromAccount),
                new AccountNumber(toAccount),
                new Money(amount, Currency.getInstance(currency)));
        return ProposalView.of(proposal);
    }

    private Account ownedAccount(String accountNumber, ToolContext context) {
        CustomerId customer = customer(context);
        return accounts.findByNumber(new AccountNumber(accountNumber))
                .filter(account -> account.owner().equals(customer))
                .orElseThrow(() -> new IllegalArgumentException("no account " + accountNumber));
    }

    private static CustomerId customer(ToolContext context) {
        return (CustomerId) context.getContext().get(CUSTOMER);
    }

    private static Instant startOf(LocalDate day) {
        return day.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
