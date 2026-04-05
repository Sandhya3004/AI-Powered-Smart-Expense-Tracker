package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal savings;
    private Double savingsRate;
    private Double budgetUsedPercentage;
    private BigDecimal remainingBudget;
    private BigDecimal monthlyBudget;
    private Map<String, Double> categoryBreakdown;
    private List<MonthlyTrend> monthlyTrends;
    private List<RecentTransaction> recentTransactions;
    private List<UpcomingBill> upcomingBills;
    private FinancialHealth financialHealth;
    private Integer totalTransactions;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private String month;
        private String year;
        private BigDecimal income;
        private BigDecimal expense;
        private Integer transactionCount;
    }
}
