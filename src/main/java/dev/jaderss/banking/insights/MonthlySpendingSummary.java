package dev.jaderss.banking.insights;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record MonthlySpendingSummary(
        YearMonth month,
        String currency,
        Map<Category, BigDecimal> spentByCategory,
        BigDecimal totalSpent,
        BigDecimal totalIncome,
        String headline) {}
