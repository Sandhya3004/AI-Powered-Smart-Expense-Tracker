package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private BigDecimal totalBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal savings;
    private List<RecentTransaction> recentTransactions;
    private List<BudgetStatus> budgetStatus;
    private List<UpcomingBill> upcomingBills;
    private List<SavingsGoalDTO> savingsGoals;
    private Map<String, Object> financialHealth;
}
