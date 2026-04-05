package com.expense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.expense.entity.User;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate financial insight using Gemini AI
     */
    public String generateFinancialInsight(User user, Map<String, BigDecimal> categorySpending, String topCategory, BigDecimal totalSpending) {
        try {
            // Prepare the prompt for Gemini
            String prompt = buildFinancialInsightPrompt(user, categorySpending, topCategory, totalSpending);
            
            // Call Gemini API
            String response = callGeminiAPI(prompt);
            
            // Extract the insight from response
            return extractInsightFromResponse(response);
            
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return generateFallbackInsight(categorySpending, topCategory, totalSpending);
        }
    }

    private String buildFinancialInsightPrompt(User user, Map<String, BigDecimal> categorySpending, String topCategory, BigDecimal totalSpending) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("As a financial advisor, analyze the following spending data for user ");
        prompt.append(user.getId());
        prompt.append(":\n\n");
        prompt.append("Total Spending: ₹").append(totalSpending).append("\n");
        prompt.append("Top Category: ").append(topCategory).append(" (₹").append(categorySpending.get(topCategory)).append(")\n");
        prompt.append("Category Breakdown:\n");
        
        categorySpending.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    prompt.append("- ").append(entry.getKey()).append(": ₹").append(entry.getValue()).append("\n");
                });
        
        prompt.append("\nProvide a concise, actionable financial insight (max 2 sentences) about this spending pattern.");
        prompt.append("Focus on practical advice for saving money or optimizing spending.");
        prompt.append("Consider the spending patterns and provide specific recommendations.");
        
        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) {
        try {
            // Create request body
            Map<String, Object> request = Map.of(
                "contents", Map.of(
                    "parts", Map.of(
                        "text", prompt
                    )
                ),
                "generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 150
                )
            );

            // Make API call
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            
            // For now, return a mock response
            return generateMockResponse(prompt);
            
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw e;
        }
    }

    private String generateMockResponse(String prompt) {
        // Generate contextual mock responses based on prompt content
        if (prompt.contains("Food") || prompt.contains("Dining")) {
            return "Your food expenses are quite high. Consider meal planning and cooking at home to reduce dining costs by 20-30%.";
        } else if (prompt.contains("Shopping") || prompt.contains("Retail")) {
            return "Shopping expenses are significant. Create a shopping list and stick to it to avoid impulse purchases.";
        } else if (prompt.contains("Transport") || prompt.contains("Travel")) {
            return "Transportation costs are substantial. Consider carpooling or public transport to save on commuting expenses.";
        } else if (prompt.contains("Entertainment")) {
            return "Entertainment spending is notable. Look for free or low-cost alternatives and set a monthly entertainment budget.";
        } else if (prompt.contains("Bills") || prompt.contains("Utilities")) {
            return "Your utility bills are consistent. Review your plans and consider energy-saving measures to reduce costs.";
        } else {
            return "Your spending patterns look reasonable. Continue tracking expenses and maintaining your current budget discipline.";
        }
    }

    private String extractInsightFromResponse(String response) {
        try {
            // Parse Gemini response and extract the insight
            // For now, just return the response as-is
            return response.substring(0, Math.min(response.length(), 200));
        } catch (Exception e) {
            log.error("Error extracting insight from Gemini response", e);
            return "Unable to process AI insight at this time.";
        }
    }

    private String generateFallbackInsight(Map<String, BigDecimal> categorySpending, String topCategory, BigDecimal totalSpending) {
        if (totalSpending.compareTo(new BigDecimal("50000")) > 0) {
            return "Your monthly spending is quite high. Focus on reducing expenses in your top category: " + topCategory;
        } else if (totalSpending.compareTo(new BigDecimal("30000")) > 0) {
            return "Your spending is moderate. Consider setting a stricter budget for " + topCategory + " expenses.";
        } else {
            return "Your spending patterns look good. Continue maintaining your current financial discipline.";
        }
    }

    /**
     * Process voice transcript using Gemini AI
     */
    public Map<String, Object> processVoice(String transcript) {
        try {
            String prompt = buildVoiceProcessingPrompt(transcript);
            
            Map<String, Object> request = Map.of(
                "contents", Map.of(
                    "parts", Map.of(
                        "text", prompt
                    )
                )
            );

            String response = webClientBuilder.build()
                    .post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(response);
            
        } catch (Exception e) {
            log.error("Error processing voice with Gemini", e);
            // Return fallback data
            return Map.of(
                "success", false,
                "error", "AI service unavailable: " + e.getMessage(),
                "amount", 0.0,
                "description", "Voice Expense",
                "category", "Other",
                "date", java.time.LocalDate.now().toString()
            );
        }
    }

    /**
     * Process receipt image using Gemini AI
     */
    public Map<String, Object> processReceipt(String imageData, String filename) {
        try {
            String prompt = buildReceiptProcessingPrompt(filename);
            
            Map<String, Object> request = Map.of(
                "contents", Map.of(
                    "parts", Map.of(
                        "text", prompt,
                        "inline_data", Map.of(
                            "mime_type", "image/jpeg",
                            "data", imageData
                        )
                    )
                )
            );

            String response = webClientBuilder.build()
                    .post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(response);
            
        } catch (Exception e) {
            log.error("Error processing receipt with Gemini", e);
            return Map.of(
                "success", false,
                "error", "AI service unavailable: " + e.getMessage(),
                "amount", 0.0,
                "description", "Receipt Expense",
                "category", "Other",
                "date", java.time.LocalDate.now().toString()
            );
        }
    }

    private String buildVoiceProcessingPrompt(String transcript) {
        return String.format("""
            You are a financial expense parser. Extract structured information from the following voice transcript:
            
            Transcript: "%s"
            
            Extract and return a JSON object with these exact keys:
            - amount: numeric value (float)
            - description: brief description (string)
            - category: one of [Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Healthcare, Education, Personal Care, Other]
            - merchant: store/merchant name (string, optional)
            - date: expense date in YYYY-MM-DD format (use today if not specified)
            
            If you cannot extract specific information, make reasonable assumptions based on context.
            Always return valid JSON format.
            """, transcript);
    }

    private String buildReceiptProcessingPrompt(String filename) {
        return String.format("""
            You are a financial receipt parser. Extract structured information from this receipt image.
            
            Filename: "%s"
            
            Extract and return a JSON object with these exact keys:
            - amount: total amount (float)
            - description: brief description (string)
            - category: one of [Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Healthcare, Education, Personal Care, Other]
            - merchant: store/merchant name (string)
            - date: receipt date in YYYY-MM-DD format (use today if not found)
            
            Look for items, prices, totals, store names, dates.
            Always return valid JSON format.
            """, filename);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGeminiResponse(String response) {
        try {
            Map<String, Object> geminiResponse = objectMapper.readValue(response, Map.class);
            
            // Extract the text content from Gemini response
            Object contents = geminiResponse.get("contents");
            if (contents instanceof java.util.List) {
                java.util.List<?> contentsList = (java.util.List<?>) contents;
                if (!contentsList.isEmpty()) {
                    return createFallbackResponse("Empty response from AI");
                }
                
                Map<String, Object> firstContent = (Map<String, Object>) contentsList.get(0);
                if (firstContent.containsKey("parts")) {
                    java.util.List<?> parts = (java.util.List<?>) firstContent.get("parts");
                    if (!parts.isEmpty()) {
                        return createFallbackResponse("No parts in AI response");
                    }
                    
                    Map<String, Object> firstPart = (Map<String, Object>) parts.get(0);
                    if (firstPart.containsKey("text")) {
                        String text = (String) firstPart.get("text");
                        return parseTextResponse(text);
                    }
                }
            }
            
            return createFallbackResponse("Invalid AI response structure");
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return createFallbackResponse("Failed to parse AI response: " + e.getMessage());
        }
    }

    private Map<String, Object> parseTextResponse(String text) {
        try {
            // Try to parse as JSON first
            if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
                return objectMapper.readValue(text, Map.class);
            }
            
            // Fallback to simple extraction
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            // Extract amount using regex
            java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("(?:\\$|₹|rs|rupees?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)");
            java.util.regex.Matcher amountMatcher = amountPattern.matcher(text.toLowerCase());
            if (amountMatcher.find()) {
                result.put("amount", Double.parseDouble(amountMatcher.group(1).replace(",", "")));
            } else {
                result.put("amount", 0.0);
            }
            
            // Extract category
            String lowerText = text.toLowerCase();
            if (lowerText.contains("food") || lowerText.contains("restaurant") || lowerText.contains("coffee")) {
                result.put("category", "Food & Dining");
            } else if (lowerText.contains("gas") || lowerText.contains("petrol") || lowerText.contains("uber")) {
                result.put("category", "Transportation");
            } else if (lowerText.contains("shopping") || lowerText.contains("amazon") || lowerText.contains("walmart")) {
                result.put("category", "Shopping");
            } else {
                result.put("category", "Other");
            }
            
            // Extract merchant
            java.util.regex.Pattern merchantPattern = java.util.regex.Pattern.compile("(?:at|from|in)\\s+([A-Za-z\\s&'-]+)");
            java.util.regex.Matcher merchantMatcher = merchantPattern.matcher(text);
            if (merchantMatcher.find()) {
                result.put("merchant", merchantMatcher.group(1).trim());
            } else {
                result.put("merchant", "Unknown");
            }
            
            // Set description
            result.put("description", "AI Parsed Expense");
            result.put("date", java.time.LocalDate.now().toString());
            
            return result;
            
        } catch (Exception e) {
            return createFallbackResponse("Error parsing text: " + e.getMessage());
        }
    }

    private Map<String, Object> createFallbackResponse(String error) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("success", false);
        fallback.put("error", error);
        fallback.put("amount", 0.0);
        fallback.put("description", "Voice Expense");
        fallback.put("category", "Other");
        fallback.put("date", java.time.LocalDate.now().toString());
        return fallback;
    }

    /**
     * Generate financial insights based on user's financial data
     */
    public Map<String, Object> generateFinancialInsight(Map<String, Object> financialData) {
        try {
            String prompt = buildFinancialInsightPrompt(financialData);
            
            Map<String, Object> request = Map.of(
                "contents", Map.of(
                    "parts", Map.of(
                        "text", prompt
                    )
                )
            );

            String response = webClientBuilder.build()
                    .post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Gemini financial insight response received");
            
            // Parse the response
            Map<String, Object> result = parseGeminiResponse(response);
            result.put("success", true);
            return result;
            
        } catch (Exception e) {
            log.error("Error generating financial insight with Gemini", e);
            return Map.of(
                "success", false,
                "error", "Failed to generate financial insight: " + e.getMessage(),
                "insight", "Unable to generate AI insights at this time. Please try again later."
            );
        }
    }

    private String buildFinancialInsightPrompt(Map<String, Object> data) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("As a financial advisor, analyze the following financial data and provide personalized insights:\n\n");
        
        if (data.containsKey("monthlyExpenses")) {
            prompt.append("Monthly Expenses: $").append(data.get("monthlyExpenses")).append("\n");
        }
        if (data.containsKey("monthlyIncome")) {
            prompt.append("Monthly Income: $").append(data.get("monthlyIncome")).append("\n");
        }
        if (data.containsKey("savings")) {
            prompt.append("Monthly Savings: $").append(data.get("savings")).append("\n");
        }
        if (data.containsKey("categoryBreakdown")) {
            prompt.append("Spending by Category: ").append(data.get("categoryBreakdown")).append("\n");
        }
        
        prompt.append("\nProvide actionable financial advice in 3-4 bullet points covering:\n");
        prompt.append("1. Spending optimization\n");
        prompt.append("2. Savings improvement\n");
        prompt.append("3. Investment suggestions\n");
        prompt.append("4. Budget recommendations\n\n");
        prompt.append("Keep the advice concise, practical, and encouraging.");
        
        return prompt.toString();
    }
}
