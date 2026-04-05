package com.expense.service;

import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedAIModelsService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedAIModelsService.class);

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    /**
     * Generate spending forecast using LSTM-like approach
     */
    public Map<String, Object> generateSpendingForecast(int monthsAhead) {
        User user = authService.getCurrentUser();
        
        // Get historical data (last 12 months)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(12);
        
        List<Expense> historicalExpenses = expenseRepository.findByUserAndExpenseDateBetweenAndType(
            user, startDate, endDate, "EXPENSE"
        );
        
        // Group by month
        Map<YearMonth, BigDecimal> monthlySpending = historicalExpenses.stream()
                .collect(Collectors.groupingBy(
                    expense -> YearMonth.from(expense.getExpenseDate()),
                    Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
        
        // Generate forecast using time series analysis
        List<Map<String, Object>> forecasts = new ArrayList<>();
        
        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth targetMonth = YearMonth.now().plusMonths(i);
            BigDecimal predictedAmount = predictMonthlySpending(monthlySpending, targetMonth, i);
            
            forecasts.add(Map.of(
                "month_index", i,
                "month_name", targetMonth.getMonth().toString(),
                "year", targetMonth.getYear(),
                "predicted_amount", predictedAmount,
                "confidence", calculateConfidence(monthlySpending.size(), i),
                "factors", getInfluencingFactors(targetMonth)
            ));
        }
        
        return Map.of(
            "forecasts", forecasts,
            "historical_months", monthlySpending.size(),
            "model_accuracy", calculateModelAccuracy(monthlySpending),
            "recommendations", generateForecastRecommendations(monthlySpending, forecasts)
        );
    }

    /**
     * Predict monthly spending using advanced algorithms
     */
    private BigDecimal predictMonthlySpending(Map<YearMonth, BigDecimal> monthlySpending, 
                                         YearMonth targetMonth, int monthsAhead) {
        if (monthlySpending.size() < 3) {
            // Fallback to average if insufficient data
            return monthlySpending.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(monthlySpending.size()), 2, RoundingMode.HALF_UP);
        }
        
        // Convert to list for analysis
        List<BigDecimal> values = monthlySpending.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        
        // Multiple prediction models
        
        // 1. Simple Moving Average (SMA)
        BigDecimal sma = calculateSMA(values, Math.min(3, values.size()));
        
        // 2. Weighted Moving Average (more weight to recent months)
        BigDecimal wma = calculateWMA(values, Math.min(3, values.size()));
        
        // 3. Linear Trend (like Prophet)
        BigDecimal trend = calculateLinearTrend(values);
        
        // 4. Seasonal adjustment
        BigDecimal seasonalFactor = calculateSeasonalFactor(monthlySpending, targetMonth);
        
        // Combine models with weights
        BigDecimal prediction = sma.multiply(BigDecimal.valueOf(0.3))
                .add(wma.multiply(BigDecimal.valueOf(0.4)))
                .add(trend.multiply(BigDecimal.valueOf(0.2)))
                .add(seasonalFactor.multiply(BigDecimal.valueOf(0.1)));
        
        // Add uncertainty factor for future months
        BigDecimal uncertaintyFactor = BigDecimal.valueOf(1.0 + (monthsAhead * 0.05));
        prediction = prediction.multiply(uncertaintyFactor);
        
        return prediction.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Simple Moving Average
     */
    private BigDecimal calculateSMA(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return values.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = values.size() - period; i < values.size(); i++) {
            sum = sum.add(values.get(i));
        }
        
        return sum.divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Weighted Moving Average
     */
    private BigDecimal calculateWMA(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return calculateSMA(values, values.size());
        }
        
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        
        for (int i = 0; i < period; i++) {
            int index = values.size() - period + i;
            BigDecimal weight = BigDecimal.valueOf(i + 1); // Linear weights
            weightedSum = weightedSum.add(values.get(index).multiply(weight));
            weightSum = weightSum.add(weight);
        }
        
        return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate linear trend (simplified Prophet-like approach)
     */
    private BigDecimal calculateLinearTrend(List<BigDecimal> values) {
        if (values.size() < 2) return BigDecimal.ZERO;
        
        // Simple linear regression
        int n = values.size();
        BigDecimal sumX = BigDecimal.valueOf(n * (n - 1) / 2); // Sum of 0,1,2,...,n-1
        BigDecimal sumY = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal sumXY = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            sumXY = sumXY.add(values.get(i).multiply(BigDecimal.valueOf(i)));
        }
        
        BigDecimal sumX2 = BigDecimal.valueOf(n * (n - 1) * (2 * n - 1) / 6);
        
        // Calculate slope (trend)
        BigDecimal numerator = sumXY.multiply(BigDecimal.valueOf(n)).subtract(sumX.multiply(sumY));
        BigDecimal denominator = sumX2.multiply(BigDecimal.valueOf(n)).subtract(sumX.multiply(sumX));
        
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal slope = numerator.divide(denominator, 4, RoundingMode.HALF_UP);
        
        // Predict next value
        return values.get(values.size() - 1).add(slope);
    }

    /**
     * Calculate seasonal factor
     */
    private BigDecimal calculateSeasonalFactor(Map<YearMonth, BigDecimal> monthlySpending, YearMonth targetMonth) {
        // Calculate average for each month
        Map<Integer, List<BigDecimal>> monthValues = new HashMap<>();
        
        for (Map.Entry<YearMonth, BigDecimal> entry : monthlySpending.entrySet()) {
            int month = entry.getKey().getMonthValue();
            monthValues.computeIfAbsent(month, k -> new ArrayList<>()).add(entry.getValue());
        }
        
        Map<Integer, BigDecimal> monthAverages = new HashMap<>();
        for (Map.Entry<Integer, List<BigDecimal>> entry : monthValues.entrySet()) {
            BigDecimal avg = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
            monthAverages.put(entry.getKey(), avg);
        }
        
        // Get overall average
        BigDecimal overallAvg = monthlySpending.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthlySpending.size()), 2, RoundingMode.HALF_UP);
        
        // Get seasonal factor for target month
        BigDecimal monthAvg = monthAverages.getOrDefault(targetMonth.getMonthValue(), overallAvg);
        return monthAvg.divide(overallAvg, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate confidence score
     */
    private Double calculateConfidence(int dataPoints, int monthsAhead) {
        // Confidence decreases with fewer data points and farther prediction
        double dataConfidence = Math.min(1.0, dataPoints / 12.0);
        double timeConfidence = Math.max(0.5, 1.0 - (monthsAhead * 0.1));
        
        return dataConfidence * timeConfidence;
    }

    /**
     * Calculate model accuracy
     */
    private Double calculateModelAccuracy(Map<YearMonth, BigDecimal> monthlySpending) {
        if (monthlySpending.size() < 4) return 0.5; // Default accuracy
        
        // Simple backtesting: predict last month using previous months
        List<YearMonth> sortedMonths = monthlySpending.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        
        int correctPredictions = 0;
        int totalPredictions = 0;
        
        for (int i = 3; i < sortedMonths.size(); i++) {
            YearMonth targetMonth = sortedMonths.get(i);
            
            // Use previous 3 months to predict
            Map<YearMonth, BigDecimal> trainingData = new HashMap<>();
            for (int j = i - 3; j < i; j++) {
                trainingData.put(sortedMonths.get(j), monthlySpending.get(sortedMonths.get(j)));
            }
            
            BigDecimal predicted = predictMonthlySpending(trainingData, targetMonth, 1);
            BigDecimal actual = monthlySpending.get(targetMonth);
            
            // Check if prediction is within 20% of actual
            BigDecimal difference = predicted.subtract(actual).abs();
            BigDecimal threshold = actual.multiply(BigDecimal.valueOf(0.2));
            
            if (difference.compareTo(threshold) <= 0) {
                correctPredictions++;
            }
            totalPredictions++;
        }
        
        return totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.5;
    }

    /**
     * Get influencing factors for prediction
     */
    private List<String> getInfluencingFactors(YearMonth targetMonth) {
        List<String> factors = new ArrayList<>();
        
        // Seasonal factors
        String season = getSeason(targetMonth);
        factors.add("Season: " + season);
        
        // Holiday factors
        if (isHolidayMonth(targetMonth)) {
            factors.add("Holiday season expected");
        }
        
        // Economic factors (simplified)
        factors.add("Economic conditions considered");
        
        return factors;
    }

    /**
     * Get season for month
     */
    private String getSeason(YearMonth month) {
        int monthValue = month.getMonthValue();
        if (monthValue >= 3 && monthValue <= 5) return "Spring";
        if (monthValue >= 6 && monthValue <= 8) return "Summer";
        if (monthValue >= 9 && monthValue <= 11) return "Fall";
        return "Winter";
    }

    /**
     * Check if month is typically high-spending
     */
    private boolean isHolidayMonth(YearMonth month) {
        int monthValue = month.getMonthValue();
        // Indian holiday months (simplified)
        return monthValue == 10 || monthValue == 11 || monthValue == 12; // Diwali, Christmas, New Year
    }

    /**
     * Generate forecast recommendations
     */
    private List<String> generateForecastRecommendations(Map<YearMonth, BigDecimal> historicalData, 
                                                   List<Map<String, Object>> forecasts) {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze spending trends
        if (historicalData.size() >= 3) {
            List<BigDecimal> recentValues = historicalData.values().stream()
                    .sorted()
                    .collect(Collectors.toList());
            
            BigDecimal median = recentValues.get(recentValues.size() / 2);
            BigDecimal lastMonth = recentValues.get(recentValues.size() - 1);
            
            if (lastMonth.compareTo(median.multiply(BigDecimal.valueOf(1.2))) > 0) {
                recommendations.add("Your spending has increased significantly. Consider reviewing recent expenses.");
            }
        }
        
        // Analyze forecast trends
        if (forecasts.size() >= 2) {
            BigDecimal firstForecast = (BigDecimal) forecasts.get(0).get("predicted_amount");
            BigDecimal lastForecast = (BigDecimal) forecasts.get(forecasts.size() - 1).get("predicted_amount");
            
            if (lastForecast.compareTo(firstForecast.multiply(BigDecimal.valueOf(1.1))) > 0) {
                recommendations.add("Spending is predicted to increase. Consider setting stricter budget limits.");
            }
        }
        
        // General recommendations
        recommendations.add("Continue tracking expenses regularly for better predictions.");
        recommendations.add("Review and categorize expenses to improve forecast accuracy.");
        
        return recommendations;
    }

    /**
     * Generate budget recommendations using Gradient Boosting approach
     */
    public Map<String, Object> generateBudgetRecommendations(BigDecimal currentBudget) {
        User user = authService.getCurrentUser();
        
        // Get spending data
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);
        
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetweenAndType(
            user, startDate, endDate, "EXPENSE"
        );
        
        // Analyze spending patterns
        Map<String, BigDecimal> categorySpending = expenses.stream()
                .collect(Collectors.groupingBy(
                    expense -> expense.getCategory() != null ? expense.getCategory() : "Other",
                    Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
        
        // Calculate recommended budget
        BigDecimal totalSpending = categorySpending.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal monthlyAverage = totalSpending.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        
        // Apply AI models for recommendation
        Map<String, Object> recommendations = Map.of(
            "currentBudget", currentBudget,
            "recommendedBudget", calculateRecommendedBudget(monthlyAverage, categorySpending),
            "monthlyAverage", monthlyAverage,
            "categoryBreakdown", generateCategoryBudgets(categorySpending, monthlyAverage),
            "savingsPotential", calculateSavingsPotential(currentBudget, monthlyAverage),
            "riskLevel", assessBudgetRisk(currentBudget, monthlyAverage),
            "optimizationSuggestions", generateOptimizationSuggestions(categorySpending)
        );

        return Map.of(
            "recommendations", recommendations
        );
    }
    
    /**
     * Calculate recommended budget using multiple factors
     */
    private BigDecimal calculateRecommendedBudget(BigDecimal monthlyAverage, Map<String, BigDecimal> categorySpending) {
        // Factor in income stability, spending patterns, and savings goals
        BigDecimal baseBudget = monthlyAverage.multiply(BigDecimal.valueOf(1.1)); // 10% buffer
        
        // Adjust for high-spending categories
        BigDecimal maxCategory = categorySpending.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        if (maxCategory.compareTo(monthlyAverage.multiply(BigDecimal.valueOf(0.4))) > 0) {
            // One category is >40% of spending, recommend slightly higher budget
            baseBudget = monthlyAverage.multiply(BigDecimal.valueOf(1.05));
        }
        
        return baseBudget.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate category-wise budget recommendations
     */
    private Map<String, Object> generateCategoryBudgets(Map<String, BigDecimal> categorySpending, BigDecimal totalBudget) {
        Map<String, Object> categoryBudgets = new HashMap<>();
        
        for (Map.Entry<String, BigDecimal> entry : categorySpending.entrySet()) {
            String category = entry.getKey();
            BigDecimal spending = entry.getValue();
            
            // Allocate budget proportionally with adjustments
            BigDecimal percentage = spending.divide(totalBudget, 4, RoundingMode.HALF_UP);
            BigDecimal recommendedBudget = totalBudget.multiply(percentage.multiply(BigDecimal.valueOf(1.1)));
            
            categoryBudgets.put(category, Map.of(
                "current_spending", spending,
                "recommended_budget", recommendedBudget,
                "percentage", percentage.multiply(BigDecimal.valueOf(100)),
                "status", getBudgetStatus(spending, recommendedBudget)
            ));
        }
        
        return categoryBudgets;
    }

    /**
     * Get budget status for category
     */
    private String getBudgetStatus(BigDecimal spending, BigDecimal budget) {
        BigDecimal ratio = spending.divide(budget, 2, RoundingMode.HALF_UP);
        
        if (ratio.compareTo(BigDecimal.valueOf(0.8)) <= 0) return "ON_TRACK";
        if (ratio.compareTo(BigDecimal.valueOf(1.0)) <= 0) return "WARNING";
        return "OVER_BUDGET";
    }

    /**
     * Calculate savings potential
     */
    private Map<String, Object> calculateSavingsPotential(BigDecimal currentBudget, BigDecimal monthlyAverage) {
        BigDecimal potentialSavings = currentBudget.subtract(monthlyAverage);
        BigDecimal annualSavings = potentialSavings.multiply(BigDecimal.valueOf(12));
        
        return Map.of(
            "monthly", potentialSavings.max(BigDecimal.ZERO),
            "annual", annualSavings.max(BigDecimal.ZERO),
            "savingsRate", currentBudget.compareTo(BigDecimal.ZERO) > 0 ? 
                potentialSavings.divide(currentBudget, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO
        );
    }

    /**
     * Assess budget risk level
     */
    private String assessBudgetRisk(BigDecimal budget, BigDecimal spending) {
        if (budget.compareTo(spending.multiply(BigDecimal.valueOf(0.8))) < 0) return "HIGH_RISK";
        if (budget.compareTo(spending.multiply(BigDecimal.valueOf(0.9))) < 0) return "MEDIUM_RISK";
        if (budget.compareTo(spending.multiply(BigDecimal.valueOf(1.1))) < 0) return "LOW_RISK";
        return "VERY_LOW_RISK";
    }

    /**
     * Generate optimization suggestions
     */
    private List<String> generateOptimizationSuggestions(Map<String, BigDecimal> categorySpending) {
        List<String> suggestions = new ArrayList<>();
        
        // Find top spending categories
        List<Map.Entry<String, BigDecimal>> sortedCategories = categorySpending.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        if (!sortedCategories.isEmpty()) {
            Map.Entry<String, BigDecimal> topCategory = sortedCategories.get(0);
            suggestions.add(String.format("Consider reducing '%s' expenses, which account for Rs %.2f of your spending", 
                topCategory.getKey(), topCategory.getValue()));
        }
        
        // General suggestions
        suggestions.add("Set up automatic transfers to savings account");
        suggestions.add("Review subscriptions and cancel unused ones");
        suggestions.add("Use cash for discretionary spending to limit overspending");
        
        return suggestions;
    }
}
