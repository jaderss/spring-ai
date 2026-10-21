package dev.jaderss.banking.core;

import static dev.jaderss.banking.core.BankingTestSupport.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dev.jaderss.banking.core.TransferRejectedException.Reason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BankingTestSupport.class)
class CoreBankingTest {

    private static final Instant SEPTEMBER = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant OCTOBER = Instant.parse("2026-10-01T00:00:00Z");
    private static final AccountNumber ADA_CHECKING = new AccountNumber("ACME-CHK-0001");
    private static final AccountNumber ADA_SAVINGS = new AccountNumber("ACME-SAV-0001");
    private static final AccountNumber ADA_EURO = new AccountNumber("ACME-CHK-0003");
    private static final AccountNumber BRUNO_CHECKING = new AccountNumber("ACME-CHK-0002");

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private TransactionRepository transactions;

    @Autowired
    private TransferService transferService;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void seededBalancesEqualTheSumOfTheirTransactions() {
        assertThat(jdbc.sql("""
                        select a.number from account a
                        left join account_transaction t on t.account_number = a.number
                        where a.number like 'ACME-%'
                        group by a.number, a.balance
                        having a.balance <> coalesce(sum(t.amount), 0)
                        """).query(String.class).list()).isEmpty();
    }

    @Test
    void listsAllAccountsOfOneCustomer() {
        assertThat(accounts.findByOwner(new CustomerId("CUST-0001")))
                .extracting(Account::number, Account::type, Account::balance)
                .containsExactly(
                        tuple(ADA_CHECKING, AccountType.CHECKING, Money.of("8912.26", "USD")),
                        tuple(ADA_EURO, AccountType.CHECKING, Money.of("500.00", "EUR")),
                        tuple(ADA_SAVINGS, AccountType.SAVINGS, Money.of("10012.50", "USD")));
    }

    @Test
    void findsTransactionsInsideDateRangeNewestFirst() {
        assertThat(transactions.search(TransactionSearch.between(
                        ADA_CHECKING, Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-07T00:00:00Z"))))
                .extracting(Transaction::description)
                .containsExactly("Mock Fuel Station", "Example Coffee Roasters", "Example Corp Payroll");
    }

    @Test
    void searchFiltersByMerchantIgnoringCase() {
        assertThat(transactions.search(new TransactionSearch(ADA_CHECKING, SEPTEMBER, OCTOBER, "grocery", null)))
                .extracting(Transaction::amount)
                .containsExactly(Money.of("-64.10", "USD"), Money.of("-86.40", "USD"));
    }

    @Test
    void searchFiltersByAmountRegardlessOfSign() {
        assertThat(transactions.search(
                        new TransactionSearch(ADA_CHECKING, SEPTEMBER, OCTOBER, null, new BigDecimal("-4.75"))))
                .extracting(Transaction::description)
                .containsExactly("Example Coffee Roasters");
    }

    @Test
    void searchTreatsLikeWildcardsAsPlainText() {
        assertThat(transactions.search(new TransactionSearch(ADA_CHECKING, SEPTEMBER, OCTOBER, "%", null)))
                .isEmpty();
    }

    @Test
    void transferMovesMoneyAndRecordsBothSides() {
        Transfer transfer = transferService.transfer(ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));

        assertThat(transfer.createdAt()).isEqualTo(NOW);
        assertThat(balanceOf(ADA_CHECKING)).isEqualTo(Money.of("8812.26", "USD"));
        assertThat(balanceOf(ADA_SAVINGS)).isEqualTo(Money.of("10112.50", "USD"));
        assertThat(postedNow(ADA_CHECKING))
                .singleElement()
                .extracting(Transaction::description, Transaction::amount)
                .containsExactly("Transfer to ACME-SAV-0001", Money.of("-100", "USD"));
        assertThat(postedNow(ADA_SAVINGS))
                .singleElement()
                .extracting(Transaction::description, Transaction::amount)
                .containsExactly("Transfer from ACME-CHK-0001", Money.of("100", "USD"));
    }

    @Test
    void transferMayEmptyTheAccountExactly() {
        transferService.transfer(BRUNO_CHECKING, ADA_CHECKING, Money.of("708.00", "USD"));

        assertThat(balanceOf(BRUNO_CHECKING)).isEqualTo(Money.of("0", "USD"));
    }

    @Test
    void insufficientFundsRejectTransferAndChangeNothing() {
        assertRejected(BRUNO_CHECKING, ADA_CHECKING, Money.of("708.01", "USD"), Reason.INSUFFICIENT_FUNDS);

        assertThat(balanceOf(BRUNO_CHECKING)).isEqualTo(Money.of("708.00", "USD"));
        assertThat(postedNow(BRUNO_CHECKING)).isEmpty();
    }

    @Test
    void rejectsTransferIntoAccountOfAnotherCurrency() {
        assertRejected(ADA_CHECKING, ADA_EURO, Money.of("10", "USD"), Reason.CURRENCY_MISMATCH);
    }

    @Test
    void rejectsTransferToUnknownAccount() {
        assertRejected(
                ADA_CHECKING, new AccountNumber("ACME-CHK-9999"), Money.of("10", "USD"), Reason.ACCOUNT_NOT_FOUND);
    }

    @Test
    void rejectsTransferToSameAccount() {
        assertRejected(ADA_CHECKING, ADA_CHECKING, Money.of("10", "USD"), Reason.SAME_ACCOUNT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10"})
    void rejectsAmountThatIsNotPositive(String amount) {
        assertRejected(ADA_CHECKING, ADA_SAVINGS, Money.of(amount, "USD"), Reason.NOT_POSITIVE);
    }

    private void assertRejected(AccountNumber from, AccountNumber to, Money amount, Reason reason) {
        assertThatThrownBy(() -> transferService.transfer(from, to, amount))
                .isInstanceOfSatisfying(
                        TransferRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(reason));
    }

    private Money balanceOf(AccountNumber number) {
        return accounts.findByNumber(number).orElseThrow().balance();
    }

    private List<Transaction> postedNow(AccountNumber number) {
        return transactions.search(TransactionSearch.between(number, NOW, NOW.plusSeconds(1)));
    }
}
