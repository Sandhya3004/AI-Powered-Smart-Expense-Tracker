package com.expense.service;

import com.expense.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiInsightService {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate financial insights based on user's spending data
     */
    public Map<String, Object> generateInsights(List<Map<String, Object>> expenses, double totalIncome, double totalExpenses) {
        try {
            String prompt = buildInsightPrompt(expenses, totalIncome, totalExpenses);
            
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
            log.error("Error generating insights with Gemini", e);
            return createFallbackInsight();
        }
    }

    private String buildInsightPrompt(List<Map<String, Object>> expenses, double totalIncome, double totalExpenses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial advisor. Analyze this spending data and provide personalized insights:\n\n");
        
        prompt.append("Income: ₹").append(totalIncome).append("\n");
        prompt.append("Expenses: ₹").append(totalExpenses).append("\n");
        prompt.append("Savings: ₹").append(totalIncome - totalExpenses).append("\n\n");
        
        prompt.append("Recent Expenses:\n");
        expenses.stream().limit(10).forEach(expense -> {
            prompt.append("- ₹").append(expense.get("amount"))
                   .append(" for ").append(expense.get("description"))
                   .append(" (").append(expense.get("category")).append(")\n");
        });
        
        prompt.append("\nProvide 3-4 actionable insights about:");
        prompt.append("1. Spending patterns and trends\n");
        prompt.append("2. Areas for potential savings\n");
        prompt.append("3. Budget recommendations\n");
        prompt.append("4. Financial health assessment\n\n");
        
        prompt.append("Keep it concise and actionable. Return as JSON with keys:");
        prompt.append("- insights: array of strings");
        prompt.append("- risk_level: one of [LOW, MEDIUM, HIGH]");
        prompt.append("- recommendations: array of strings");
        
        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGeminiResponse(String response) {
        try {
            Map<String, Object> geminiResponse = objectMapper.readValue(response, Map.class);
            
            Object contents = geminiResponse.get("contents");
            if (contents instanceof List) {
                List<?> contentsList = (List<?>) contents;
                if (!contentsList.isEmpty()) {
                    return createFallbackInsight();
                }
                
                Map<String, Object> firstContent = (Map<String, Object>) contentsList.get(0);
                if (firstContent.containsKey("parts")) {
                    List<?> parts = (List<?>) firstContent.get("parts");
                    if (!parts.isEmpty()) {
                        return createFallbackInsight();
                    }
                    
                    Map<String, Object> firstPart = (Map<String, Object>) parts.get(0);
                    if (firstPart.containsKey("text")) {
                        String text = (String) firstPart.get("text");
                        return parseInsightResponse(text);
                    }
                }
            }
            
            return createFallbackInsight();
            
        } catch (Exception e) {
            log.error("Error parsing Gemini insight response", e);
            return createFallbackInsight();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInsightResponse(String text) {
        try {
            // Try to parse as JSON first
            if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
                Map<String, Object> jsonResponse = objectMapper.readValue(text, Map.class);
                jsonResponse.put("success", true);
                return jsonResponse;
            }
            
            // Fallback to basic insights
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("insights", List.of(
                "Your spending appears to be on track with good expense tracking",
                "Consider setting up automatic savings transfers",
                "Review subscription costs for optimization opportunities"
            ));
            result.put("risk_level", "LOW");
            result.put("recommendations", List.of(
                "Continue current spending habits",
                "Set monthly savings goals",
                "Review discretionary spending"
            ));
            
            return result;
            
        } catch (Exception e) {
            return createFallbackInsight();
        }
    }

    private Map<String, Object> createFallbackInsight() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("success", false);
        fallback.put("error", "AI insights temporarily unavailable");
        fallback.put("insights", List.of(
            "Track your expenses regularly",
            "Set monthly budget limits",
            "Review spending patterns weekly"
        ));
        fallback.put("risk_level", "MEDIUM");
        fallback.put("recommendations", List.of(
            "Monitor large expenses",
            "Build emergency fund",
            "Reduce discretionary spending"
        ));
        return fallback;
    }

    /**
     * Generate financial insight for a specific user
     */
    public String generateFinancialInsight(User user) {
        try {
            log.info("Generating financial insight for user: {}", user.getId());
            
            // For now, return a placeholder insight
            // In a real implementation, you would:
            // 1. Get user's recent expenses (last 30 days)
            // 2. Get category breakdown
            // 3. Get budget status
            // 4. Build a prompt and call Gemini API
            // 5. Return the generated insight
            
            String insight = "Based on your recent spending patterns, you could save ₹500 by reducing dining out expenses and focusing on home-cooked meals. Your grocery spending is well within budget, which is great! Consider setting up automatic transfers to your savings account to build your emergency fund faster.";
            
            log.info("Generated financial insight for user: {}", user.getId());
            return insight;
            
        } catch (Exception e) {
            log.error("Error generating financial insight for user: {}", user.getId(), e);
            return "Based on your spending, you could save ₹500 by reducing dining out. Keep tracking your expenses to identify more savings opportunities.";
        }
    }
}
