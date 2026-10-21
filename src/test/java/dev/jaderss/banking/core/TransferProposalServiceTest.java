package dev.jaderss.banking.core;

import static dev.jaderss.banking.core.BankingTestSupport.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.jaderss.banking.core.TransferRejectedException.Reason;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BankingTestSupport.class)
class TransferProposalServiceTest {

    private static final CustomerId ADA = new CustomerId("CUST-0001");
    private static final CustomerId BRUNO = new CustomerId("CUST-0002");
    private static final AccountNumber ADA_CHECKING = new AccountNumber("ACME-CHK-0001");
    private static final AccountNumber ADA_SAVINGS = new AccountNumber("ACME-SAV-0001");
    private static final AccountNumber BRUNO_CHECKING = new AccountNumber("ACME-CHK-0002");

    @Autowired
    private TransferProposalService service;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private TransferProposalRepository proposalRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransferService transferService;

    @Test
    void proposingMovesNoMoneyAndExpiresAfterTenMinutes() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));

        assertThat(proposal.expiresAt()).isEqualTo(NOW.plus(TransferProposalService.PROPOSAL_LIFETIME));
        assertThat(balanceOf(ADA_CHECKING)).isEqualTo(Money.of("8912.26", "USD"));
        assertThat(service.pending(ADA)).containsExactly(proposal);
    }

    @Test
    void cannotProposeFromAnotherCustomersAccount() {
        assertThatThrownBy(() -> service.propose(ADA, BRUNO_CHECKING, ADA_CHECKING, Money.of("10", "USD")))
                .isInstanceOfSatisfying(
                        TransferRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.ACCOUNT_NOT_FOUND));
    }

    @Test
    void confirmationPostsTheTransferOnce() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));

        Confirmation confirmation = service.confirm(ADA, proposal.id(), "key-0000001");

        assertThat(confirmation.replayed()).isFalse();
        assertThat(balanceOf(ADA_CHECKING)).isEqualTo(Money.of("8812.26", "USD"));
        assertThat(service.pending(ADA)).isEmpty();
    }

    @Test
    void replayedConfirmationReturnsTheOriginalTransferWithoutPostingAgain() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));
        Confirmation first = service.confirm(ADA, proposal.id(), "key-0000002");

        Confirmation replay = service.confirm(ADA, proposal.id(), "key-0000002");

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.transfer()).isEqualTo(first.transfer());
        assertThat(balanceOf(ADA_CHECKING)).isEqualTo(Money.of("8812.26", "USD"));
    }

    @Test
    void confirmingAgainWithAnotherKeyIsRejected() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));
        service.confirm(ADA, proposal.id(), "key-0000003");

        assertRejected(() -> service.confirm(ADA, proposal.id(), "key-0000004"), Reason.ALREADY_CONFIRMED);
        assertThat(balanceOf(ADA_CHECKING)).isEqualTo(Money.of("8812.26", "USD"));
    }

    @Test
    void reusingAnIdempotencyKeyForAnotherProposalIsRejected() {
        TransferProposal first = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("10", "USD"));
        TransferProposal second = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("20", "USD"));
        service.confirm(ADA, first.id(), "key-0000008");

        assertRejected(() -> service.confirm(ADA, second.id(), "key-0000008"), Reason.IDEMPOTENCY_KEY_REUSED);
    }

    @Test
    void anotherCustomerCannotConfirmTheProposal() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));

        assertRejected(() -> service.confirm(BRUNO, proposal.id(), "key-0000005"), Reason.PROPOSAL_NOT_FOUND);
    }

    @Test
    void expiredProposalCannotBeConfirmed() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("100", "USD"));
        Clock later = Clock.fixed(proposal.expiresAt(), ZoneOffset.UTC);
        TransferProposalService serviceLater =
                new TransferProposalService(accounts, proposalRepository, transferRepository, transferService, later);

        assertRejected(() -> serviceLater.confirm(ADA, proposal.id(), "key-0000006"), Reason.PROPOSAL_EXPIRED);
    }

    @Test
    void failedConfirmationLeavesTheProposalPending() {
        TransferProposal proposal = service.propose(ADA, ADA_CHECKING, ADA_SAVINGS, Money.of("8912.27", "USD"));

        assertRejected(() -> service.confirm(ADA, proposal.id(), "key-0000007"), Reason.INSUFFICIENT_FUNDS);
        assertThat(service.pending(ADA)).containsExactly(proposal);
    }

    private void assertRejected(Runnable call, Reason reason) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        TransferRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(reason));
    }

    private Money balanceOf(AccountNumber number) {
        return accounts.findByNumber(number).orElseThrow().balance();
    }
}
