package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.service.AIExpenseCategorizationService;
import com.expense.service.AdvancedAIModelsService;
import com.expense.service.ExpenseService;
import com.expense.service.GeminiInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    private final AIExpenseCategorizationService categorizationService;
    private final AdvancedAIModelsService aiModelsService;
    private final ExpenseService expenseService;
    private final GeminiInsightService geminiInsightService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, String>>> chatWithAI(@RequestBody Map<String, String> request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("No authenticated user for AI chat request");
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("User not authenticated"));
            }
            
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Message is required"));
            }
            
            log.info("AI chat request from user {}: {}", currentUser.getId(), message);
            
            // Fetch real user financial data
            YearMonth currentMonth = YearMonth.now();
            BigDecimal totalExpenses = expenseService.getTotalExpensesByMonth(currentUser, currentMonth);
            BigDecimal totalIncome = expenseService.getTotalIncomeByMonth(currentUser, currentMonth);
            BigDecimal savings = totalIncome.subtract(totalExpenses);
            
            // Get category breakdown
            List<Map<String, Object>> categoryData = expenseService.getCategoryBreakdown(currentUser, currentMonth);
            Map<String, BigDecimal> categorySpending = new HashMap<>();
            String topCategory = "Other";
            BigDecimal topAmount = BigDecimal.ZERO;
            
            for (Map<String, Object> cat : categoryData) {
                String category = (String) cat.get("category");
                BigDecimal amount = (BigDecimal) cat.getOrDefault("amount", BigDecimal.ZERO);
                categorySpending.put(category, amount);
                if (amount.compareTo(topAmount) > 0) {
                    topAmount = amount;
                    topCategory = category;
                }
            }
            
            // Generate contextual AI response using real data
            String aiResponse = generateAIResponseWithRealData(message, totalExpenses, totalIncome, savings, categorySpending, topCategory);
            
            Map<String, String> response = Map.of("reply", aiResponse);
            return ResponseEntity.ok(ApiResponse.success(response, "AI response generated successfully"));
            
        } catch (Exception e) {
            log.error("AI chat error", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to generate AI response: " + e.getMessage()));
        }
    }

    private String generateAIResponseWithRealData(String message, BigDecimal totalExpenses, 
            BigDecimal totalIncome, BigDecimal savings, Map<String, BigDecimal> categorySpending, String topCategory) {
        
        String lowerMessage = message.toLowerCase();
        
        // Calculate savings rate
        double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0 
            ? savings.doubleValue() / totalIncome.doubleValue() * 100 
            : 0;
        
        if (lowerMessage.contains("spending") || lowerMessage.contains("expense") || lowerMessage.contains("how much")) {
            return String.format("This month you've spent ₹%s. Your top spending category is %s at ₹%s. " +
                "To reduce expenses, consider reviewing your %s purchases.", 
                totalExpenses, topCategory, categorySpending.getOrDefault(topCategory, BigDecimal.ZERO), topCategory.toLowerCase());
        } 
        else if (lowerMessage.contains("savings") || lowerMessage.contains("save")) {
            if (savings.compareTo(BigDecimal.ZERO) > 0) {
                return String.format("Great job! You've saved ₹%s this month (%.1f%% savings rate). " +
                    "Keep it up to build your financial cushion.", savings, savingsRate);
            } else {
                return String.format("You're currently spending ₹%s more than you earned this month. " +
                    "Try to reduce expenses in %s to get back on track.", 
                    savings.abs(), topCategory.toLowerCase());
            }
        }
        else if (lowerMessage.contains("income") || lowerMessage.contains("earned")) {
            return String.format("Your total income this month is ₹%s. " +
                "Make sure you're tracking all income sources accurately.", totalIncome);
        }
        else if (lowerMessage.contains("budget") || lowerMessage.contains("advice") || lowerMessage.contains("tip")) {
            return String.format("Based on your spending of ₹%s with %s as your top category, " +
                "I recommend setting a monthly budget of ₹%s and tracking %s expenses more closely.",
                totalExpenses, topCategory, totalIncome.multiply(new BigDecimal("0.8")), topCategory.toLowerCase());
        }
        else if (lowerMessage.contains("category") || lowerMessage.contains("breakdown")) {
            StringBuilder sb = new StringBuilder("Your spending breakdown:\n");
            categorySpending.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append(String.format("• %s: ₹%s\n", e.getKey(), e.getValue())));
            return sb.toString();
        }
        else {
            return String.format("I can see you've spent ₹%s this month with a savings of ₹%s. " +
                "Ask me about your spending, savings, budget advice, or category breakdown!", 
                totalExpenses, savings);
        }
    }

    @PostMapping("/categorize")
    public ResponseEntity<String> categorizeExpense(@RequestBody Map<String, Object> request) {
        String description = (String) request.get("description");
        String merchant = (String) request.get("merchant");
        BigDecimal amount = request.get("amount") != null ? new BigDecimal(request.get("amount").toString()) : null;
        
        log.info("Categorizing expense: {} - {} - {}", description, merchant, amount);
        
        String category = categorizationService.categorizeExpense(description, merchant, amount);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/learn")
    public ResponseEntity<Void> learnFromCorrection(@RequestBody Map<String, String> request) {
        String description = (String) request.get("description");
        String merchant = (String) request.get("merchant");
        String correctCategory = (String) request.get("correctCategory");
        
        categorizationService.learnFromCorrection(description, merchant, correctCategory);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getCategorySuggestions(@RequestParam String description, 
                                                      @RequestParam String merchant, 
                                                      @RequestParam BigDecimal amount) {
        List<String> suggestions = categorizationService.getCategorySuggestions(description, merchant, amount);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getCategorizationMetrics() {
        Map<String, Object> metrics = categorizationService.getCategorizationMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/batch-categorize")
    public ResponseEntity<List<String>> batchCategorizeExpenses(@RequestBody List<Map<String, Object>> expenses) {
        List<String> categories = categorizationService.batchCategorizeExpenses(expenses);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/forecast")
    public ResponseEntity<Map<String, Object>> generateSpendingForecast(@RequestParam(defaultValue = "3") int monthsAhead) {
        Map<String, Object> forecast = aiModelsService.generateSpendingForecast(monthsAhead);
        return ResponseEntity.ok(forecast);
    }

    @GetMapping("/budget-recommendations")
    public ResponseEntity<Map<String, Object>> generateBudgetRecommendations(@RequestParam BigDecimal currentBudget) {
        Map<String, Object> recommendations = aiModelsService.generateBudgetRecommendations(currentBudget);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getSpendingAnalytics(@RequestParam(required = false) String startDate,
                                                           @RequestParam(required = false) String endDate) {
        // This would need to be implemented in AdvancedAIModelsService
        return ResponseEntity.ok().build();
    }

    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeBudget(@RequestBody Map<String, Object> request) {
        // This would need to be implemented in AdvancedAIModelsService
        return ResponseEntity.ok().build();
    }

    @GetMapping("/model-accuracy")
    public ResponseEntity<Map<String, Object>> getModelAccuracy() {
        // This would need to be implemented in AdvancedAIModelsService
        return ResponseEntity.ok().build();
    }

    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getAIInsights() {
        Map<String, Object> insights = Map.of(
            "categorizationAccuracy", categorizationService.getCategorizationMetrics(),
            "forecastAvailable", true,
            "budgetOptimization", true,
            "voiceCommandsSupported", true,
            "ocrProcessing", true
        );
        return ResponseEntity.ok(insights);
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody Map<String, Object> request) {
        // This would need to be implemented for learning system
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAvailableCategories() {
        List<String> categories = List.of(
            "Food & Dining", "Transportation", "Shopping", "Entertainment",
            "Bills & Utilities", "Healthcare", "Education", "Travel",
            "Investments", "Savings", "Other"
        );
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/patterns")
    public ResponseEntity<Map<String, Object>> getSpendingPatterns() {
        // This would need to be implemented in AdvancedAIModelsService
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> processOcr(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            log.info("Processing OCR for file: {}", file.getOriginalFilename());
            // This would need to be implemented in a service
            Map<String, Object> result = Map.of(
                "message", "OCR processing not yet implemented",
                "status", "pending"
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR failed", e);
            Map<String, Object> error = Map.of(
                "error", "OCR processing failed: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(error);
        }
    }
}