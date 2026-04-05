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
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIExpenseCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(AIExpenseCategorizationService.class);

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    // Predefined category keywords for classification
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new HashMap<>();
    
    static {
        CATEGORY_KEYWORDS.put("Food & Dining", Arrays.asList(
            "restaurant", "food", "dining", "cafe", "coffee", "pizza", "burger", 
            "meal", "lunch", "dinner", "breakfast", "swiggy", "zomato", "dominos",
            "mcdonalds", "kfc", "starbucks", "cafe coffee day", "barista"
        ));
        
        CATEGORY_KEYWORDS.put("Transportation", Arrays.asList(
            "uber", "ola", "taxi", "cab", "metro", "bus", "train", "petrol", 
            "diesel", "fuel", "gas", "parking", "toll", "auto", "rickshaw"
        ));
        
        CATEGORY_KEYWORDS.put("Shopping", Arrays.asList(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "clothes", "shoes", 
            "fashion", "electronics", "gadgets", "mobile", "laptop", "shopping"
        ));
        
        CATEGORY_KEYWORDS.put("Entertainment", Arrays.asList(
            "netflix", "prime", "hotstar", "spotify", "youtube", "movies", "cinema", 
            "theatre", "concert", "gaming", "steam", "playstation", "xbox"
        ));
        
        CATEGORY_KEYWORDS.put("Bills & Utilities", Arrays.asList(
            "electricity", "water", "gas", "internet", "phone", "mobile", "rent", 
            "maintenance", "society", "broadband", "recharge", "bill payment"
        ));
        
        CATEGORY_KEYWORDS.put("Healthcare", Arrays.asList(
            "hospital", "doctor", "medicine", "pharmacy", "medical", "health", 
            "insurance", "clinic", "diagnostic", "apollo", "practo"
        ));
        
        CATEGORY_KEYWORDS.put("Education", Arrays.asList(
            "school", "college", "tuition", "course", "books", "stationery", 
            "fees", "education", "udemy", "coursera", "byju's"
        ));
        
        CATEGORY_KEYWORDS.put("Investments", Arrays.asList(
            "mutual fund", "sip", "stocks", "shares", "demat", "trading", 
            "investment", "fd", "rd", "ppf", "epf", "nps"
        ));
        
        CATEGORY_KEYWORDS.put("Travel", Arrays.asList(
            "hotel", "flight", "train", "booking", "trip", "vacation", "holiday", 
            "oyo", "makemytrip", "goibibo", "cleartrip"
        ));
    }

    /**
     * Categorize expense using AI/NLP techniques
     */
    public String categorizeExpense(String description, String merchant, BigDecimal amount) {
        try {
            // Primary classification using keyword matching
            String primaryCategory = classifyByKeywords(description, merchant);
            
            // Secondary classification using pattern recognition
            String patternCategory = classifyByPattern(description, merchant, amount);
            
            // Tertiary classification using amount-based heuristics
            String amountCategory = classifyByAmount(amount);
            
            // Combine results with confidence scoring
            return combineClassificationResults(primaryCategory, patternCategory, amountCategory);
            
        } catch (Exception e) {
            log.error("Error in AI categorization", e);
            return "Other";
        }
    }

    /**
     * Classify based on keyword matching
     */
    private String classifyByKeywords(String description, String merchant) {
        String text = (description + " " + merchant).toLowerCase();
        
        Map<String, Integer> categoryScores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            
            int score = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase())) {
                    score += keyword.split(" ").length; // Higher score for longer keywords
                }
            }
            categoryScores.put(category, score);
        }
        
        return categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("Other");
    }

    /**
     * Classify based on patterns
     */
    private String classifyByPattern(String description, String merchant, BigDecimal amount) {
        String text = (description + " " + merchant).toLowerCase();
        
        // Pattern-based classification
        if (Pattern.compile("\\b(recharge|bill|payment)\\b").matcher(text).find()) {
            if (text.contains("mobile") || text.contains("phone")) {
                return "Bills & Utilities";
            }
        }
        
        if (Pattern.compile("\\b(booking|ticket|reservation)\\b").matcher(text).find()) {
            if (text.contains("hotel") || text.contains("flight") || text.contains("train")) {
                return "Travel";
            }
        }
        
        if (Pattern.compile("\\b(subscription|membership)\\b").matcher(text).find()) {
            if (text.contains("netflix") || text.contains("prime") || text.contains("spotify")) {
                return "Entertainment";
            }
        }
        
        return null;
    }

    /**
     * Classify based on amount heuristics
     */
    private String classifyByAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return "Food & Dining"; // Small amounts likely food/daily expenses
        } else if (amount.compareTo(BigDecimal.valueOf(5000)) < 0) {
            return "Shopping"; // Medium amounts likely shopping
        } else if (amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            return "Bills & Utilities"; // Larger amounts likely bills
        } else {
            return "Investments"; // Very large amounts likely investments
        }
    }

    /**
     * Combine classification results with confidence scoring
     */
    private String combineClassificationResults(String primary, String pattern, String amount) {
        Map<String, Integer> votes = new HashMap<>();
        
        if (primary != null && !primary.equals("Other")) {
            votes.put(primary, votes.getOrDefault(primary, 0) + 3); // Primary gets highest weight
        }
        if (pattern != null && !pattern.equals("Other")) {
            votes.put(pattern, votes.getOrDefault(pattern, 0) + 2); // Pattern gets medium weight
        }
        if (amount != null && !amount.equals("Other")) {
            votes.put(amount, votes.getOrDefault(amount, 0) + 1); // Amount gets lowest weight
        }
        
        return votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Other");
    }

    /**
     * Learn from user corrections and improve categorization
     */
    public void learnFromCorrection(String description, String merchant, String correctCategory) {
        try {
            User user = authService.getCurrentUser();
            
            // Store learning data (in real implementation, this would go to a learning database)
            log.info("Learning from user correction: {} {} -> {}", description, merchant, correctCategory);
            
            // Update user's personal category preferences
            updateUserCategoryPreferences(user, description, merchant, correctCategory);
            
        } catch (Exception e) {
            log.error("Error learning from correction", e);
        }
    }

    /**
     * Update user's personal category preferences
     */
    private void updateUserCategoryPreferences(User user, String description, String merchant, String category) {
        // This would store user-specific preferences in a separate table
        // For now, just log the preference
        log.info("User {} prefers category {} for {}", user.getEmail(), category, description);
    }

    /**
     * Get category suggestions for a transaction
     */
    public List<String> getCategorySuggestions(String description, String merchant, BigDecimal amount) {
        String primaryCategory = categorizeExpense(description, merchant, amount);
        
        List<String> suggestions = new ArrayList<>();
        suggestions.add(primaryCategory);
        
        // Add related categories based on the primary category
        switch (primaryCategory) {
            case "Food & Dining":
                suggestions.addAll(Arrays.asList("Shopping", "Transportation"));
                break;
            case "Transportation":
                suggestions.addAll(Arrays.asList("Bills & Utilities", "Shopping"));
                break;
            case "Shopping":
                suggestions.addAll(Arrays.asList("Food & Dining", "Entertainment"));
                break;
            case "Entertainment":
                suggestions.addAll(Arrays.asList("Food & Dining", "Shopping"));
                break;
            case "Bills & Utilities":
                suggestions.addAll(Arrays.asList("Transportation", "Healthcare"));
                break;
            case "Healthcare":
                suggestions.addAll(Arrays.asList("Bills & Utilities", "Shopping"));
                break;
        }
        
        return suggestions.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * Batch categorize multiple expenses
     */
    public List<String> batchCategorizeExpenses(List<Map<String, Object>> expenses) {
        return expenses.stream()
                .map(expense -> categorizeExpense(
                    (String) expense.get("description"),
                    (String) expense.get("merchant"),
                    (BigDecimal) expense.get("amount")
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get categorization accuracy metrics
     */
    public Map<String, Object> getCategorizationMetrics() {
        User user = authService.getCurrentUser();
        
        // Get user's recent expenses
        List<Expense> recentExpenses = expenseRepository.findByUserOrderByExpenseDateDesc(user)
                .stream()
                .limit(100)
                .collect(Collectors.toList());
        
        // Calculate metrics (in real implementation, compare with user corrections)
        long totalExpenses = recentExpenses.size();
        long autoCategorized = recentExpenses.stream()
                .filter(expense -> expense.getSource() != null && expense.getSource().equals("ai"))
                .count();
        
        double accuracy = totalExpenses > 0 ? (double) autoCategorized / totalExpenses : 0.0;
        
        return Map.of(
            "totalExpenses", totalExpenses,
            "autoCategorized", autoCategorized,
            "accuracy", accuracy,
            "improvementSuggestions", getImprovementSuggestions(recentExpenses)
        );
    }

    /**
     * Get improvement suggestions based on categorization patterns
     */
    private List<String> getImprovementSuggestions(List<Expense> expenses) {
        List<String> suggestions = new ArrayList<>();
        
        // Analyze uncategorized expenses
        long uncategorizedCount = expenses.stream()
                .filter(expense -> expense.getCategory() == null || expense.getCategory().equals("Other"))
                .count();
        
        if (uncategorizedCount > expenses.size() * 0.3) {
            suggestions.add("Consider adding more specific descriptions for better categorization");
        }
        
        suggestions.add("Regularly review and correct AI categorizations to improve accuracy");
        suggestions.add("Add custom keywords for frequently misclassified merchants");
        
        return suggestions;
    }
}
