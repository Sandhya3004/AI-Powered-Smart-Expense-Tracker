package com.expense.service;

import com.expense.dto.*;
import com.expense.entity.*;
import com.expense.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final BillRepository billRepository;
    private final BillReminderRepository billReminderRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;

    public BigDecimal getTotalBalance(User user) {
        try {
            // Calculate total income from INCOME type expenses
            BigDecimal totalIncome = expenseRepository.findByUserAndType(user, "INCOME")
                    .stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate total expenses from EXPENSE type expenses
            BigDecimal totalExpenses = expenseRepository.findByUserAndType(user, "EXPENSE")
                    .stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return totalIncome.subtract(totalExpenses);
        } catch (Exception e) {
            log.error("Error calculating total balance", e);
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getMonthlyIncome(User user) {
        try {
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
            
            return expenseRepository.findByUserAndExpenseDateBetweenAndType(user, startOfMonth, endOfMonth, "INCOME")
                    .stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Error calculating monthly income", e);
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getMonthlyExpenses(User user) {
        try {
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
            
            return expenseRepository.findByUserAndExpenseDateBetweenAndType(user, startOfMonth, endOfMonth, "EXPENSE")
                    .stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Error calculating monthly expenses", e);
            return BigDecimal.ZERO;
        }
    }

    public List<RecentTransaction> getRecentTransactions(User user) {
        try {
            Pageable pageable = PageRequest.of(0, 5);
            List<Expense> expenses = expenseRepository.findByUser(user, pageable).getContent();
            return expenses.stream()
                    .limit(5)
                    .map(expense -> RecentTransaction.builder()
                            .id(expense.getId())
                            .date(expense.getExpenseDate().atStartOfDay())
                            .description(expense.getDescription())
                            .amount(expense.getAmount())
                            .category(expense.getCategory())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting recent transactions", e);
            return new ArrayList<>();
        }
    }

    public List<BudgetStatus> getBudgetStatus(User user) {
        try {
            List<Budget> budgets = budgetRepository.findByUser(user);
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
            
            return budgets.stream()
                    .map(budget -> {
                        // Calculate actual spending for this category
                        BigDecimal spent = expenseRepository
                                .findByUserAndExpenseDateBetweenAndCategory(user, startOfMonth, endOfMonth, budget.getCategory())
                                .stream()
                                .filter(e -> "EXPENSE".equals(e.getType()))
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        BigDecimal remaining = budget.getAmount().subtract(spent);
                        double percentageUsed = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ?
                                spent.doubleValue() / budget.getAmount().doubleValue() * 100 : 0;
                        
                        return BudgetStatus.builder()
                                .category(budget.getCategory())
                                .budgetAmount(budget.getAmount())
                                .spent(spent)
                                .remaining(remaining)
                                .percentageUsed(percentageUsed)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting budget status", e);
            return new ArrayList<>();
        }
    }

    public List<UpcomingBill> getUpcomingBills(User user, int daysAhead) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(daysAhead);
            
            List<BillReminder> bills = billReminderRepository.findByUserAndDueDateBetweenOrderByDueDateAsc(
                    user, today, futureDate);
            
            return bills.stream()
                    .map(bill -> {
                        String status = bill.isPaid() ? "PAID" : 
                                       bill.getDueDate().isBefore(today) ? "OVERDUE" : "PENDING";
                        int daysUntilDue = (int) java.time.temporal.ChronoUnit.DAYS.between(today, bill.getDueDate());
                        
                        return UpcomingBill.builder()
                                .id(bill.getId())
                                .title(bill.getBillName())
                                .amount(bill.getAmount())
                                .dueDate(bill.getDueDate())
                                .category(bill.getCategory())
                                .isPaid(bill.isPaid())
                                .status(status)
                                .daysUntilDue(daysUntilDue)
                                .billReminderId(bill.getId())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting upcoming bills", e);
            return new ArrayList<>();
        }
    }

    public List<SavingsGoalDTO> getSavingsGoals(User user) {
        try {
            List<com.expense.entity.SavingsGoal> goals = savingsGoalRepository.findByUserOrderByTargetDateAsc(user);
            return goals.stream()
                    .map(goal -> SavingsGoalDTO.builder()
                            .id(goal.getId())
                            .name(goal.getName())
                            .targetAmount(goal.getTargetAmount())
                            .currentAmount(goal.getCurrentAmount())
                            .deadline(goal.getDeadline())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting savings goals", e);
            return new ArrayList<>();
        }
    }

    public FinancialHealth getFinancialHealth(User user) {
        try {
            BigDecimal totalIncome = getMonthlyIncome(user);
            BigDecimal totalExpenses = getMonthlyExpenses(user);
            BigDecimal savings = totalIncome.subtract(totalExpenses);
            double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0 ?
                    savings.doubleValue() / totalIncome.doubleValue() * 100 : 0;
            
            int score = savingsRate > 30 ? 90 : savingsRate > 20 ? 80 : savingsRate > 10 ? 60 : 40;
            String rating = savingsRate > 30 ? "Excellent" : savingsRate > 20 ? "Good" : savingsRate > 10 ? "Fair" : "Needs Improvement";
            
            return FinancialHealth.builder()
                    .score(score)
                    .rating(rating)
                    .savingsRate(BigDecimal.valueOf(savingsRate))
                    .debtRatio(BigDecimal.ZERO)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating financial health", e);
            return FinancialHealth.builder().build();
        }
    }

    public Map<String, Object> getFinancialHealthMap(User user) {
        FinancialHealth financialHealth = getFinancialHealth(user);
        Map<String, Object> financialHealthMap = new HashMap<>();
        financialHealthMap.put("score", financialHealth.getScore());
        financialHealthMap.put("rating", financialHealth.getRating());
        financialHealthMap.put("savingsRate", financialHealth.getSavingsRate());
        financialHealthMap.put("debtRatio", financialHealth.getDebtRatio());
        return financialHealthMap;
    }

    public DashboardResponse getDashboardResponse(User user) {
        try {
            // Reload user from database to ensure it's a managed entity
            User managedUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found in database"));
            
            // Calculate monthly income and expenses
            BigDecimal totalIncome = getMonthlyIncome(managedUser);
            BigDecimal totalExpense = getMonthlyExpenses(managedUser);
            BigDecimal savings = totalIncome.subtract(totalExpense);
            BigDecimal totalBalance = getTotalBalance(managedUser);

            // Get budget info for budgetUsedPercentage and remainingBudget
            List<BudgetStatus> budgetStatus = getBudgetStatus(managedUser);
            BigDecimal totalBudget = budgetStatus.stream()
                    .map(BudgetStatus::getBudgetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSpent = budgetStatus.stream()
                    .map(BudgetStatus::getSpent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remainingBudget = totalBudget.subtract(totalSpent);
            Double budgetUsedPercentage = totalBudget.compareTo(BigDecimal.ZERO) > 0
                    ? totalSpent.doubleValue() / totalBudget.doubleValue() * 100
                    : 0.0;

            // Category breakdown as Map<String, Double>
            Map<String, Double> categoryBreakdown = getCategoryBreakdown(managedUser);

            // Monthly trends (last 6 months)
            List<DashboardResponse.MonthlyTrend> monthlyTrends = getMonthlyTrends(managedUser, 6);

            // Recent transactions and upcoming bills
            List<RecentTransaction> recentTransactions = getRecentTransactions(managedUser);
            List<UpcomingBill> upcomingBills = getUpcomingBills(managedUser, 30);

            // Financial health
            FinancialHealth financialHealth = getFinancialHealth(managedUser);

            return DashboardResponse.builder()
                    .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                    .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                    .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
                    .savings(savings != null ? savings : BigDecimal.ZERO)
                    .budgetUsedPercentage(budgetUsedPercentage != null ? budgetUsedPercentage : 0.0)
                    .remainingBudget(remainingBudget != null ? remainingBudget : BigDecimal.ZERO)
                    .categoryBreakdown(categoryBreakdown != null ? categoryBreakdown : new HashMap<>())
                    .monthlyTrends(monthlyTrends != null ? monthlyTrends : new ArrayList<>())
                    .recentTransactions(recentTransactions != null ? recentTransactions : new ArrayList<>())
                    .upcomingBills(upcomingBills != null ? upcomingBills : new ArrayList<>())
                    .financialHealth(financialHealth != null ? financialHealth : FinancialHealth.builder().build())
                    .message("Dashboard data retrieved successfully")
                    .build();
        } catch (Exception e) {
            log.error("Error getting dashboard response", e);
            // Return empty response on error (no 500 error)
            return DashboardResponse.builder()
                    .totalBalance(BigDecimal.ZERO)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .savings(BigDecimal.ZERO)
                    .budgetUsedPercentage(0.0)
                    .remainingBudget(BigDecimal.ZERO)
                    .categoryBreakdown(new HashMap<>())
                    .monthlyTrends(new ArrayList<>())
                    .recentTransactions(new ArrayList<>())
                    .upcomingBills(new ArrayList<>())
                    .financialHealth(FinancialHealth.builder().build())
                    .message("Error retrieving dashboard data: " + e.getMessage())
                    .build();
        }
    }

    public Map<String, Double> getCategoryBreakdown(User user) {
        try {
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

            List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetweenAndType(
                    user, startOfMonth, endOfMonth, "EXPENSE");

            return expenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory() != null ? e.getCategory() : "Other",
                            Collectors.summingDouble(e -> e.getAmount() != null ? e.getAmount().doubleValue() : 0.0)
                    ));
        } catch (Exception e) {
            log.error("Error getting category breakdown", e);
            return new HashMap<>();
        }
    }

    public List<DashboardResponse.MonthlyTrend> getMonthlyTrends(User user, int months) {
        try {
            List<DashboardResponse.MonthlyTrend> trends = new ArrayList<>();
            LocalDate now = LocalDate.now();

            for (int i = months - 1; i >= 0; i--) {
                LocalDate startOfMonth = now.minusMonths(i).withDayOfMonth(1);
                LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
                String monthName = startOfMonth.getMonth().toString().substring(0, 3);

                BigDecimal income = expenseRepository
                        .findByUserAndExpenseDateBetweenAndType(user, startOfMonth, endOfMonth, "INCOME")
                        .stream()
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal expense = expenseRepository
                        .findByUserAndExpenseDateBetweenAndType(user, startOfMonth, endOfMonth, "EXPENSE")
                        .stream()
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                trends.add(DashboardResponse.MonthlyTrend.builder()
                        .month(monthName)
                        .income(income != null ? income : BigDecimal.ZERO)
                        .expense(expense != null ? expense : BigDecimal.ZERO)
                        .build());
            }
            return trends;
        } catch (Exception e) {
            log.error("Error getting monthly trends", e);
            return new ArrayList<>();
        }
    }

    public DashboardData getDashboardData(User user) {
        try {
            BigDecimal monthlyIncome = getMonthlyIncome(user);
            BigDecimal monthlyExpenses = getMonthlyExpenses(user);
            BigDecimal savings = monthlyIncome.subtract(monthlyExpenses);
            
            return DashboardData.builder()
                    .totalBalance(getTotalBalance(user))
                    .monthlyIncome(monthlyIncome)
                    .monthlyExpenses(monthlyExpenses)
                    .savings(savings)
                    .recentTransactions(getRecentTransactions(user))
                    .budgetStatus(getBudgetStatus(user))
                    .upcomingBills(getUpcomingBills(user, 30))
                    .savingsGoals(getSavingsGoals(user))
                    .financialHealth(getFinancialHealthMap(user))
                    .build();
        } catch (Exception e) {
            log.error("Error getting dashboard data", e);
            return DashboardData.builder().build();
        }
    }
}
