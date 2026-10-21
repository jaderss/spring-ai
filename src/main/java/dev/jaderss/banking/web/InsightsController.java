package dev.jaderss.banking.web;

import dev.jaderss.banking.core.AccountNumber;
import dev.jaderss.banking.core.CustomerId;
import dev.jaderss.banking.insights.MonthlySpendingSummary;
import dev.jaderss.banking.insights.SpendingAnalyst;
import java.time.YearMonth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InsightsController {

    private final SpendingAnalyst analyst;

    public InsightsController(SpendingAnalyst analyst) {
        this.analyst = analyst;
    }

    @GetMapping("/accounts/{accountNumber}/spending")
    public MonthlySpendingSummary spending(
            @RequestHeader(Headers.CUSTOMER) String customerId,
            @PathVariable String accountNumber,
            @RequestParam YearMonth month) {
        return analyst.summarize(new CustomerId(customerId), new AccountNumber(accountNumber), month);
    }
}
