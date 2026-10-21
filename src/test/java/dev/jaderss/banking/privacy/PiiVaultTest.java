package dev.jaderss.banking.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PiiVaultTest {

    private final PiiVault vault = new PiiVault(List.of("Ada Example", "Bruno Sample"));

    @Test
    void replacesAccountNumbersAndKnownNamesWithStableTokens() {
        assertThat(vault.tokenize("Ada Example sends ACME-CHK-0001 money to ACME-CHK-0002, then ACME-CHK-0001 again"))
                .isEqualTo("NAME_1 sends ACCT_1 money to ACCT_2, then ACCT_1 again");
        assertThat(vault.tokenize("Bruno Sample and ACME-CHK-0002")).isEqualTo("NAME_2 and ACCT_2");
    }

    @Test
    void revealRestoresTheOriginalText() {
        String original = "Transfer from ACME-SAV-0001 to Bruno Sample at ACME-CHK-0002";

        assertThat(vault.reveal(vault.tokenize(original))).isEqualTo(original);
    }

    @Test
    void leavesUnknownTokensAndPlainTextAlone() {
        assertThat(vault.reveal("ACCT_9 and NAME_7 are unknown")).isEqualTo("ACCT_9 and NAME_7 are unknown");
        assertThat(vault.tokenize("Example Corp Payroll")).isEqualTo("Example Corp Payroll");
    }

    @Test
    void handlesMissingText() {
        assertThat(vault.tokenize(null)).isNull();
        assertThat(vault.reveal(null)).isNull();
    }

    @Test
    void passThroughVaultLeavesTextUnchanged() {
        assertThat(PiiVault.passThrough().tokenize("Ada Example owns ACME-CHK-0001"))
                .isEqualTo("Ada Example owns ACME-CHK-0001");
    }
}
