package com.expense.service;

import com.expense.dto.*;
import com.expense.entity.Budget;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.exception.ResourceNotFoundException;
import com.expense.repository.BudgetRepository;
import com.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final AiIntegrationService aiService;
    private final ExpenseService expenseService;
    private final GeminiService geminiService;

    @Cacheable(value = "monthlySummary", key = "#month + '_' + #user.id", unless = "#result == null")
    public MonthlySummaryDTO getMonthlySummary(YearMonth month, User user) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(user, start, end);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal budget = null;
        BigDecimal remaining = null;
        try {
            Budget userBudget = budgetRepository.findByUserAndMonth(user, month.toString()).orElse(null);
            if (userBudget != null) {
                budget = userBudget.getTotalBudget();
                remaining = budget.subtract(total);
            }
        } catch (Exception e) {
            log.error("Error fetching budget for user {}: {}", user.getId(), e.getMessage());
        }

        return MonthlySummaryDTO.builder()
                .month(month)
                .totalExpenses(total)
                .expenseCount(expenses.size())
                .budget(budget)
                .remaining(remaining)
                .build();
    }

    public List<CategoryBreakdownDTO> getCategoryBreakdown(YearMonth month, User user) {
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                user, month.atDay(1), month.atEndOfMonth());

        Map<String, BigDecimal> categorySum = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        return categorySum.entrySet().stream()
                .map(e -> new CategoryBreakdownDTO(e.getKey(), e.getValue()))
                .toList();
    }

    public PredictionDTO getMonthlyPrediction() {
        return aiService.getPrediction();
    }

    public List<Expense> detectAnomalies() {
        return aiService.detectAnomalies();
    }

    public FinancialHealth calculateFinancialHealth(User user) {
        log.info("Calculating financial health for user: {}", user.getId());
        
        // Get last 3 months data
        List<BigDecimal> last3MonthsExpenses = new ArrayList<>();
        List<BigDecimal> last3MonthsIncome = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal expenses = expenseService.getTotalExpensesByMonth(user, month);
            BigDecimal income = expenseService.getTotalIncomeByMonth(user, month);
            last3MonthsExpenses.add(expenses);
            last3MonthsIncome.add(income);
        }
        
        // Calculate averages
        BigDecimal avgExpenses = last3MonthsExpenses.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal avgIncome = last3MonthsIncome.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        
        // Calculate savings rate
        BigDecimal avgSavings = avgIncome.subtract(avgExpenses);
        BigDecimal savingsRate = avgIncome.compareTo(BigDecimal.ZERO) > 0 
            ? avgSavings.divide(avgIncome, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        // Calculate debt ratio (mock - assume no debt for now)
        BigDecimal debtRatio = BigDecimal.ZERO;
        
        // Calculate health score (0-100)
        int score = 100;
        
        // Savings rate component (40% weight)
        if (savingsRate.compareTo(new BigDecimal("20")) < 0) {
            score -= 40;
        } else if (savingsRate.compareTo(new BigDecimal("30")) < 0) {
            score -= 20;
        }
        
        // Budget adherence component (30% weight)
        double budgetAdherence = calculateBudgetAdherence(user);
        if (budgetAdherence < 80) {
            score -= 30;
        } else if (budgetAdherence < 90) {
            score -= 15;
        }
        
        // Income stability component (20% weight)
        double incomeVariability = calculateIncomeVariability(last3MonthsIncome);
        if (incomeVariability > 20) {
            score -= 20;
        } else if (incomeVariability > 10) {
            score -= 10;
        }
        
        // Expense consistency component (10% weight)
        double expenseVariability = calculateExpenseVariability(last3MonthsExpenses);
        if (expenseVariability > 15) {
            score -= 10;
        } else if (expenseVariability > 8) {
            score -= 5;
        }
        
        score = Math.max(0, Math.min(100, score));
        
        String rating = score >= 80 ? "Excellent" : 
                       score >= 60 ? "Good" : 
                       score >= 40 ? "Fair" : "Poor";
        
        List<String> recommendations = new ArrayList<>();
        if (savingsRate.compareTo(new BigDecimal("20")) < 0) {
            recommendations.add("Increase savings rate to at least 20%");
        }
        if (budgetAdherence < 80) {
            recommendations.add("Reduce expenses to stay within budget");
        }
        if (score < 60) {
            recommendations.add("Review and optimize spending patterns");
        }
        if (incomeVariability > 15) {
            recommendations.add("Work on stabilizing income sources");
        }
        
        return FinancialHealth.builder()
                .score(score)
                .rating(rating)
                .savingsRate(savingsRate)
                .debtRatio(debtRatio)
                .recommendations(recommendations)
                .lastCalculated(LocalDateTime.now().toString())
                .build();
    }

    public List<BudgetInsight> getBudgetInsights(User user) {
        log.info("Getting budget insights for user: {}", user.getId());
        
        List<Budget> budgets = budgetRepository.findByUser(user);
        List<BudgetInsight> insights = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        for (Budget budget : budgets) {
            BigDecimal spent = expenseService.getTotalByCategoryForMonth(user, budget.getCategory(), currentMonth);
            BigDecimal remaining = budget.getTotalBudget().subtract(spent);
            double percentUsed = budget.getTotalBudget().compareTo(BigDecimal.ZERO) > 0 
                ? spent.divide(budget.getTotalBudget(), 2, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;
            
            String status = percentUsed > 100 ? "OVER_BUDGET" : 
                           percentUsed > 80 ? "WARNING" : "ON_TRACK";
            
            insights.add(BudgetInsight.builder()
                    .category(budget.getCategory())
                    .budgetAmount(budget.getTotalBudget())
                    .spent(spent)
                    .remaining(remaining)
                    .percentageUsed(percentUsed)
                    .status(status)
                    .build());
        }
        
        return insights;
    }

    public List<MonthlyTrend> getMonthlyTrends(User user, int months) {
        log.info("Getting monthly trends for user: {}, months: {}", user.getId(), months);
        
        List<MonthlyTrend> trends = new ArrayList<>();
        YearMonth current = YearMonth.now();
        
        for (int i = 0; i < months; i++) {
            YearMonth ym = current.minusMonths(i);
            BigDecimal expenses = expenseService.getTotalExpensesByMonth(user, ym);
            BigDecimal income = expenseService.getTotalIncomeByMonth(user, ym);
            BigDecimal savings = income.subtract(expenses);
            
            trends.add(MonthlyTrend.builder()
                    .month(ym.toString())
                    .expenses(expenses)
                    .income(income)
                    .savings(savings)
                    .build());
        }
        
        Collections.reverse(trends);
        return trends;
    }

    public List<CategorySpending> getTopCategories(User user, int limit) {
        log.info("Getting top categories for user: {}, limit: {}", user.getId(), limit);
        
        YearMonth currentMonth = YearMonth.now();
        List<Object[]> results = expenseRepository.findCategoryTotalsForMonth(
                user.getId(), 
                currentMonth.getYear(), 
                currentMonth.getMonthValue()
        );
        
        BigDecimal totalSpending = results.stream()
                .map(arr -> (BigDecimal) arr[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return results.stream()
                .map(arr -> {
                    String category = (String) arr[0];
                    BigDecimal amount = (BigDecimal) arr[1];
                    double percentage = totalSpending.compareTo(BigDecimal.ZERO) > 0 
                        ? amount.divide(totalSpending, 2, RoundingMode.HALF_UP).doubleValue() * 100
                        : 0.0;
                    return CategorySpending.builder()
                            .category(category)
                            .amount(amount)
                            .percentage(percentage)
                            .build();
                })
                .sorted(Comparator.comparing(CategorySpending::getAmount).reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public ForecastData getForecast(User user, int monthsAhead) {
        log.info("Getting forecast for user: {}, months ahead: {}", user.getId(), monthsAhead);
        
        // Get last 6 months expenses
        List<BigDecimal> historical = expenseService.getMonthlyTotals(user, 6);
        
        // Simple moving average forecast
        BigDecimal avg = historical.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(historical.size()), 2, RoundingMode.HALF_UP);
        
        List<BigDecimal> forecast = new ArrayList<>();
        for (int i = 0; i < monthsAhead; i++) {
            // Add some variation (±5%)
            double variation = 1 + (Math.random() * 0.1 - 0.05);
            BigDecimal forecastAmount = avg.multiply(BigDecimal.valueOf(variation))
                    .setScale(2, RoundingMode.HALF_UP);
            forecast.add(forecastAmount);
        }
        
        return ForecastData.builder()
                .forecast(forecast)
                .method("Simple moving average with variation")
                .confidence(0.75)
                .description("Forecast based on last 6 months average spending")
                .build();
    }

    // Helper methods
    private double calculateBudgetAdherence(User user) {
        List<Budget> budgets = budgetRepository.findByUser(user);
        YearMonth currentMonth = YearMonth.now();
        
        if (budgets.isEmpty()) return 100.0;
        
        int onTrackCount = 0;
        for (Budget budget : budgets) {
            BigDecimal spent = expenseService.getTotalByCategoryForMonth(user, budget.getCategory(), currentMonth);
            double percentageUsed = budget.getTotalBudget().compareTo(BigDecimal.ZERO) > 0 
                ? spent.divide(budget.getTotalBudget(), 2, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;
            
            if (percentageUsed <= 100) {
                onTrackCount++;
            }
        }
        
        return (double) onTrackCount / budgets.size() * 100;
    }

    private double calculateIncomeVariability(List<BigDecimal> incomes) {
        if (incomes.isEmpty()) return 0.0;
        
        BigDecimal mean = incomes.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(incomes.size()), 2, RoundingMode.HALF_UP);
        
        double variance = incomes.stream()
                .mapToDouble(income -> {
                    double diff = income.subtract(mean).doubleValue();
                    return diff * diff;
                })
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        return mean.compareTo(BigDecimal.ZERO) > 0 
            ? (stdDev / mean.doubleValue()) * 100 
            : 0.0;
    }

    private double calculateExpenseVariability(List<BigDecimal> expenses) {
        if (expenses.isEmpty()) return 0.0;
        
        BigDecimal mean = expenses.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
        
        double variance = expenses.stream()
                .mapToDouble(expense -> {
                    double diff = expense.subtract(mean).doubleValue();
                    return diff * diff;
                })
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        return mean.compareTo(BigDecimal.ZERO) > 0 
            ? (stdDev / mean.doubleValue()) * 100 
            : 0.0;
    }
}