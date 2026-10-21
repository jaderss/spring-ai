package dev.jaderss.banking.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({"10, USD, 10.00", "10.5, USD, 10.50", "1000, JPY, 1000", "0.125, BHD, 0.125"})
    void normalizesToCurrencyScale(String amount, String currency, String expected) {
        assertThat(Money.of(amount, currency).amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void amountsDifferingOnlyInScaleAreEqual() {
        assertThat(Money.of("10", "USD")).isEqualTo(Money.of("10.00", "USD"));
    }

    @ParameterizedTest
    @CsvSource({"10.005, USD", "1000.5, JPY"})
    void rejectsMoreDecimalsThanCurrencyAllowsInsteadOfRounding(String amount, String currency) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of(amount, currency))
                .withMessageContaining(currency);
    }

    @Test
    void addsAndSubtractsInSameCurrency() {
        Money balance = Money.of("100.00", "USD");

        assertThat(balance.add(Money.of("0.10", "USD"))).isEqualTo(Money.of("100.10", "USD"));
        assertThat(balance.subtract(Money.of("100.01", "USD")).isNegative()).isTrue();
    }

    @Test
    void refusesToCombineDifferentCurrencies() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of("1", "USD").add(Money.of("1", "EUR")))
                .withMessage("cannot combine USD with EUR");
    }

    @Test
    void keepsCurrency() {
        assertThat(Money.of("5", "EUR").negate())
                .isEqualTo(new Money(new BigDecimal("-5.00"), Currency.getInstance("EUR")));
    }
}
