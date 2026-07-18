package com.DTMK.Online.Bookkeeping.Website.Project.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MonthlyStatsDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;          // Balance (income - expenses)
    private BigDecimal monthlyBudget;    // Budget target (null if unset)
    private BigDecimal budgetUsedPercent; // Expense percentage (0-100+)
    private boolean budgetWarning;        // true if >= 80%
    private boolean budgetExceeded;       // true if >= 100%
}
