package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.User;
import com.expense.service.ExpenseService;
import com.expense.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Slf4j
public class InsightsController extends BaseController {

    private final ExpenseService expenseService;
    private final GeminiService geminiService;

    @GetMapping("/gemini-insight")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGeminiInsight(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("Generating Gemini insight for user: {}", currentUser.getId());
            
            // Default to current month if not provided
            if (year == null || month == null) {
                YearMonth currentMonth = YearMonth.now();
                year = currentMonth.getYear();
                month = currentMonth.getMonthValue();
            }
            
            YearMonth targetMonth = YearMonth.of(year, month);
            
            // Get financial data for the month
            BigDecimal totalExpenses = expenseService.getTotalExpensesByMonth(currentUser, targetMonth);
            BigDecimal totalIncome = expenseService.getTotalIncomeByMonth(currentUser, targetMonth);
            BigDecimal savings = totalIncome.subtract(totalExpenses);
            
            // Get category breakdown
            Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
            // This would need to be implemented in ExpenseService
            // For now, we'll pass the basic data
            
            Map<String, Object> financialData = Map.of(
                "monthlyExpenses", totalExpenses,
                "monthlyIncome", totalIncome,
                "savings", savings,
                "categoryBreakdown", categoryBreakdown,
                "userId", currentUser.getId(),
                "month", targetMonth.toString()
            );
            
            // Generate AI insight
            Map<String, Object> insight = geminiService.generateFinancialInsight(financialData);
            
            return ResponseEntity.ok(ApiResponse.success(insight, "Financial insight generated successfully"));
            
        } catch (Exception e) {
            log.error("Error generating Gemini insight", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to generate financial insight: " + e.getMessage()));
        }
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("Getting monthly summary for user: {}", currentUser.getId());
            
            // Default to current month if not provided
            if (year == null || month == null) {
                YearMonth currentMonth = YearMonth.now();
                year = currentMonth.getYear();
                month = currentMonth.getMonthValue();
            }
            
            YearMonth targetMonth = YearMonth.of(year, month);
            
            // Get financial data
            BigDecimal totalExpenses = expenseService.getTotalExpensesByMonth(currentUser, targetMonth);
            BigDecimal totalIncome = expenseService.getTotalIncomeByMonth(currentUser, targetMonth);
            BigDecimal savings = totalIncome.subtract(totalExpenses);
            
            Map<String, Object> summary = Map.of(
                "year", year,
                "month", month,
                "totalExpenses", totalExpenses,
                "totalIncome", totalIncome,
                "savings", savings
            );
            
            return ResponseEntity.ok(ApiResponse.success(summary, "Monthly summary retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting monthly summary", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get monthly summary: " + e.getMessage()));
        }
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getCategoryBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("Getting category breakdown for user: {}", currentUser.getId());
            
            // Default to current month if not provided
            if (year == null || month == null) {
                YearMonth currentMonth = YearMonth.now();
                year = currentMonth.getYear();
                month = currentMonth.getMonthValue();
            }
            
            YearMonth targetMonth = YearMonth.of(year, month);
            
            // Get category breakdown from expense service
            java.util.List<Map<String, Object>> categoryData = expenseService.getCategoryBreakdown(currentUser, targetMonth);
            
            return ResponseEntity.ok(ApiResponse.success(categoryData, "Category breakdown retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting category breakdown", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get category breakdown: " + e.getMessage()));
        }
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("Getting financial summary for user: {}", currentUser.getId());
            
            // Default to current month if not provided
            if (year == null || month == null) {
                YearMonth currentMonth = YearMonth.now();
                year = currentMonth.getYear();
                month = currentMonth.getMonthValue();
            }
            
            YearMonth targetMonth = YearMonth.of(year, month);
            
            // Get financial data
            BigDecimal totalExpenses = expenseService.getTotalExpensesByMonth(currentUser, targetMonth);
            BigDecimal totalIncome = expenseService.getTotalIncomeByMonth(currentUser, targetMonth);
            BigDecimal savings = totalIncome.subtract(totalExpenses);
            
            // Calculate savings rate
            double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0 
                    ? savings.doubleValue() / totalIncome.doubleValue() * 100 
                    : 0.0;
            
            // Get monthly trend (last 6 months)
            java.util.List<BigDecimal> monthlyTotals = expenseService.getMonthlyTotals(currentUser, 6);
            
            Map<String, Object> summary = Map.of(
                "year", year,
                "month", month,
                "totalExpenses", totalExpenses,
                "totalIncome", totalIncome,
                "savings", savings,
                "savingsRate", Math.round(savingsRate * 100.0) / 100.0,
                "monthlyTrend", monthlyTotals,
                "financialHealth", getFinancialHealthRating(savingsRate)
            );
            
            return ResponseEntity.ok(ApiResponse.success(summary, "Financial summary retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting financial summary", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get financial summary: " + e.getMessage()));
        }
    }

    private String getFinancialHealthRating(double savingsRate) {
        if (savingsRate >= 30) {
            return "Excellent";
        } else if (savingsRate >= 20) {
            return "Good";
        } else if (savingsRate >= 10) {
            return "Fair";
        } else if (savingsRate >= 0) {
            return "Poor";
        } else {
            return "Critical";
        }
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int months) {
        try {
            User currentUser = getCurrentUser();
            log.info("Getting monthly trends for user: {} for last {} months", currentUser.getId(), months);
            
            java.util.List<BigDecimal> monthlyTotals = expenseService.getMonthlyTotals(currentUser, months);
            
            // Create trend data with month labels
            java.time.YearMonth now = java.time.YearMonth.now();
            java.util.List<Map<String, Object>> trendData = new java.util.ArrayList<>();
            
            for (int i = months - 1; i >= 0; i--) {
                java.time.YearMonth month = now.minusMonths(i);
                int index = months - 1 - i;
                BigDecimal amount = index < monthlyTotals.size() ? monthlyTotals.get(index) : BigDecimal.ZERO;
                
                Map<String, Object> monthData = Map.of(
                    "month", month.toString(),
                    "monthName", month.getMonth().toString(),
                    "year", month.getYear(),
                    "totalExpenses", amount,
                    "totalIncome", BigDecimal.ZERO // Could be enhanced to track income too
                );
                trendData.add(monthData);
            }
            
            return ResponseEntity.ok(ApiResponse.success(trendData, "Monthly trends retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting monthly trends", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get monthly trends: " + e.getMessage()));
        }
    }

    @GetMapping("/top-categories")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getTopCategories(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            User currentUser = getCurrentUser();
            log.info("Getting top {} categories for user: {}", limit, currentUser.getId());
            
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            java.util.List<Map<String, Object>> categoryData = expenseService.getCategoryBreakdown(currentUser, currentMonth);
            
            // Sort by amount and limit
            java.util.List<Map<String, Object>> sortedCategories = categoryData.stream()
                .sorted((a, b) -> {
                    BigDecimal amountA = (BigDecimal) a.getOrDefault("amount", BigDecimal.ZERO);
                    BigDecimal amountB = (BigDecimal) b.getOrDefault("amount", BigDecimal.ZERO);
                    return amountB.compareTo(amountA);
                })
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(sortedCategories, "Top categories retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting top categories", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get top categories: " + e.getMessage()));
        }
    }

    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getForecast(
            @RequestParam(defaultValue = "3") int monthsAhead) {
        try {
            User currentUser = getCurrentUser();
            log.info("Generating forecast for user: {} for next {} months", currentUser.getId(), monthsAhead);
            
            // Get historical data for prediction
            java.util.List<BigDecimal> monthlyTotals = expenseService.getMonthlyTotals(currentUser, 6);
            
            // Simple forecasting using average with trend
            BigDecimal average = monthlyTotals.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(monthlyTotals.size(), 1)), 2, java.math.RoundingMode.HALF_UP);
            
            java.util.List<Map<String, Object>> forecasts = new java.util.ArrayList<>();
            java.time.YearMonth now = java.time.YearMonth.now();
            
            for (int i = 1; i <= monthsAhead; i++) {
                java.time.YearMonth futureMonth = now.plusMonths(i);
                // Add slight variation for demo purposes
                BigDecimal forecastAmount = average.multiply(BigDecimal.valueOf(1 + (i * 0.02)));
                
                Map<String, Object> forecast = Map.of(
                    "month", futureMonth.toString(),
                    "predictedExpenses", forecastAmount,
                    "confidence", 0.75 - (i * 0.05) // Confidence decreases further out
                );
                forecasts.add(forecast);
            }
            
            Map<String, Object> result = Map.of(
                "forecasts", forecasts,
                "averageMonthlySpending", average,
                "trend", "stable"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result, "Forecast generated successfully"));
            
        } catch (Exception e) {
            log.error("Error generating forecast", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to generate forecast: " + e.getMessage()));
        }
    }

    @GetMapping("/budget-insights")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBudgetInsights() {
        try {
            User currentUser = getCurrentUser();
            log.info("Getting budget insights for user: {}", currentUser.getId());
            
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            BigDecimal totalExpenses = expenseService.getTotalExpensesByMonth(currentUser, currentMonth);
            
            // Get user's budget settings if available
            BigDecimal monthlyBudget = currentUser.getMonthlyBudget() != null 
                ? currentUser.getMonthlyBudget() 
                : BigDecimal.valueOf(2000); // Default assumption
            
            BigDecimal remaining = monthlyBudget.subtract(totalExpenses);
            double spentPercentage = monthlyBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalExpenses.doubleValue() / monthlyBudget.doubleValue() * 100
                : 0;
            
            String status;
            if (spentPercentage > 100) {
                status = "over-budget";
            } else if (spentPercentage > 80) {
                status = "warning";
            } else if (spentPercentage > 50) {
                status = "on-track";
            } else {
                status = "good";
            }
            
            Map<String, Object> insights = Map.of(
                "monthlyBudget", monthlyBudget,
                "spentAmount", totalExpenses,
                "remainingAmount", remaining,
                "spentPercentage", Math.round(spentPercentage * 100.0) / 100.0,
                "status", status,
                "daysRemaining", java.time.LocalDate.now().lengthOfMonth() - java.time.LocalDate.now().getDayOfMonth()
            );
            
            return ResponseEntity.ok(ApiResponse.success(insights, "Budget insights retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting budget insights", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get budget insights: " + e.getMessage()));
        }
    }
}
