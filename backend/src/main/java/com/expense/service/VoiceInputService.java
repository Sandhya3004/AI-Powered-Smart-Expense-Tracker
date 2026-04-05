package com.expense.service;

import com.expense.dto.VoiceInputDTO;
import com.expense.dto.ExpenseDTO;
import com.expense.dto.ExpenseResponseDTO;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.repository.ExpenseRepository;
import com.expense.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceInputService {

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;
    private final ExpenseService expenseService;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Transactional
    public Expense processVoiceInput(VoiceInputDTO voiceInput) {
        User user = authService.getCurrentUser();

        try {
            log.info("Processing voice input: {}", voiceInput.getTranscript());

            // Call AI microservice to parse the voice transcript
            Map<String, Object> aiData = callAIVoiceService(voiceInput.getTranscript());
            log.info("AI voice parsing result: {}", aiData);

            // Create ExpenseDTO with all required fields
            ExpenseDTO expenseDTO = ExpenseDTO.builder()
                    .description((String) aiData.getOrDefault("description", "Voice Expense"))
                    .amount(new java.math.BigDecimal(aiData.getOrDefault("amount", "0").toString()))
                    .category((String) aiData.getOrDefault("category", "Other"))
                    .merchant((String) aiData.getOrDefault("merchant", "Voice Input"))
                    .expenseDate(java.time.LocalDate.parse((String) aiData.getOrDefault("date", java.time.LocalDate.now().toString())))
                    .type("EXPENSE")
                    .source("voice")
                    .notes("Voice transcript: " + voiceInput.getTranscript())
                    .currency("INR")
                    .paymentType("Cash")
                    .account("Cash")
                    .status("COMPLETED")
                    .build();

            // Create the expense record using ExpenseService
            ExpenseResponseDTO expenseResponse = expenseService.createExpense(expenseDTO);
            
            // Convert back to Expense entity
            Expense expense = expenseRepository.findById(expenseResponse.getId()).orElse(null);

            log.info("Voice input processed successfully for user: {}", user.getEmail());
            return expense;

        } catch (Exception e) {
            log.error("Failed to process voice input", e);
            throw new RuntimeException("Failed to process voice input: " + e.getMessage(), e);
        }
    }
    
    public String transcribeAudio(String audioData) {
        // Mock transcription - in production, integrate with speech-to-text API
        log.info("Processing audio data for transcription");
        
        // Simulate processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Return mock transcription based on common expense patterns
        return "I spent 25 dollars on coffee at Starbucks today";
    }

    public Map<String, Object> parseExpenseFromText(String transcription) {
        Map<String, Object> expenseData = new HashMap<>();
        
        try {
            // Extract amount
            BigDecimal amount = extractAmount(transcription);
            expenseData.put("amount", amount);
            
            // Extract category
            String category = extractCategory(transcription);
            expenseData.put("category", category);
            
            // Extract description/merchant
            String description = extractDescription(transcription);
            expenseData.put("description", description);
            
            // Extract payment method
            String paymentMethod = extractPaymentMethod(transcription);
            expenseData.put("paymentMethod", paymentMethod);
            
            expenseData.put("success", true);
            
        } catch (Exception e) {
            log.error("Failed to parse expense from text: {}", transcription, e);
            expenseData.put("success", false);
            expenseData.put("error", "Failed to parse expense from voice input");
        }
        
        return expenseData;
    }
    
    private Map<String, Object> callAIVoiceService(String transcript) {
        try {
            WebClient webClient = webClientBuilder.build();
            
            // Prepare request payload
            Map<String, String> request = new HashMap<>();
            request.put("text", transcript);
            request.put("type", "voice");
            
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(request);
            
            // Call AI microservice
            Map<String, Object> response = webClient.post()
                    .uri(aiServiceUrl + "/parse-voice")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("AI voice service response: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to call AI voice service", e);
            // Fallback to basic extraction
            return fallbackVoiceExtraction(transcript);
        }
    }
    
    private Map<String, Object> fallbackVoiceExtraction(String transcript) {
        Map<String, Object> result = new HashMap<>();
        
        // Simple regex-based extraction as fallback
        java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d{1,2})?\\s*(?:dollars?|dollars?|\\$|rs|rupees?)");
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(today|yesterday|tomorrow|\\d{1,2}[\\/-]\\d{1,2}[\\/-]\\d{2,4})");
        
        // Extract amount
        java.util.regex.Matcher amountMatcher = amountPattern.matcher(transcript.toLowerCase());
        if (amountMatcher.find()) {
            String amountStr = amountMatcher.group(1).replace(",", "");
            try {
                double amount = Double.parseDouble(amountStr);
                result.put("amount", amount);
            } catch (NumberFormatException e) {
                result.put("amount", 0.0);
            }
        } else {
            result.put("amount", 0.0);
        }
        
        // Extract date
        String lowerTranscript = transcript.toLowerCase();
        if (lowerTranscript.contains("today")) {
            result.put("date", LocalDate.now().toString());
        } else if (lowerTranscript.contains("yesterday")) {
            result.put("date", LocalDate.now().minusDays(1).toString());
        } else if (lowerTranscript.contains("tomorrow")) {
            result.put("date", LocalDate.now().plusDays(1).toString());
        } else {
            result.put("date", LocalDate.now().toString());
        }
        
        // Extract merchant (simple keyword matching)
        String merchant = extractMerchantFromTranscript(transcript);
        result.put("merchant", merchant);
        
        // Extract category based on keywords
        String category = extractCategoryFromTranscript(transcript);
        result.put("category", category);
        
        // Generate description
        result.put("description", "Voice expense: " + transcript);
        
        // Set confidence
        result.put("confidence", 0.4); // Medium confidence for fallback
        
        return result;
    }
    
    private String extractMerchantFromTranscript(String transcript) {
        String lower = transcript.toLowerCase();
        
        // Common merchant keywords
        if (lower.contains("starbucks") || lower.contains("coffee")) {
            return "Starbucks";
        } else if (lower.contains("mcdonald") || lower.contains("mcdonalds")) {
            return "McDonald's";
        } else if (lower.contains("subway")) {
            return "Subway";
        } else if (lower.contains("pizza") || lower.contains("domino")) {
            return "Pizza Place";
        } else if (lower.contains("gas") || lower.contains("petrol") || lower.contains("fuel")) {
            return "Gas Station";
        } else if (lower.contains("grocery") || lower.contains("supermarket")) {
            return "Grocery Store";
        } else if (lower.contains("amazon")) {
            return "Amazon";
        } else if (lower.contains("uber") || lower.contains("lyft")) {
            return "Ride Share";
        } else {
            return "Voice Input";
        }
    }
    
    private String extractCategoryFromTranscript(String transcript) {
        String lower = transcript.toLowerCase();
        
        if (lower.contains("coffee") || lower.contains("starbucks")) {
            return "Food & Dining";
        } else if (lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast")) {
            return "Food & Dining";
        } else if (lower.contains("gas") || lower.contains("petrol") || lower.contains("fuel")) {
            return "Transportation";
        } else if (lower.contains("uber") || lower.contains("lyft") || lower.contains("taxi")) {
            return "Transportation";
        } else if (lower.contains("movie") || lower.contains("netflix")) {
            return "Entertainment";
        } else if (lower.contains("grocery") || lower.contains("supermarket")) {
            return "Groceries";
        } else if (lower.contains("amazon") || lower.contains("shopping")) {
            return "Shopping";
        } else if (lower.contains("pharmacy") || lower.contains("medicine")) {
            return "Healthcare";
        } else {
            return "Other";
        }
    }
    
    private BigDecimal extractAmount(String text) {
        // Pattern to match monetary amounts - improved regex
        Pattern pattern = Pattern.compile("(?:\\$|rs|rupees?|dollars?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:\\$|rs|rupees?|dollars?)");
        Matcher matcher = pattern.matcher(text.toLowerCase());
        
        if (matcher.find()) {
            String amountStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            amountStr = amountStr.replace(",", "").trim();
            try {
                double amount = Double.parseDouble(amountStr);
                return new BigDecimal(amount);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        } else {
            return BigDecimal.ZERO;
        }
    }
    
    private String extractCategory(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("coffee") || lowerText.contains("starbucks")) {
            return "Food & Dining";
        } else if (lowerText.contains("lunch") || lowerText.contains("dinner") || lowerText.contains("breakfast")) {
            return "Food & Dining";
        } else if (lowerText.contains("gas") || lowerText.contains("petrol") || lowerText.contains("fuel")) {
            return "Transportation";
        } else if (lowerText.contains("uber") || lowerText.contains("lyft") || lowerText.contains("taxi")) {
            return "Transportation";
        } else if (lowerText.contains("movie") || lowerText.contains("netflix") || lowerText.contains("entertainment")) {
            return "Entertainment";
        } else if (lowerText.contains("grocery") || lowerText.contains("supermarket")) {
            return "Groceries";
        } else if (lowerText.contains("amazon") || lowerText.contains("shopping")) {
            return "Shopping";
        } else if (lowerText.contains("pharmacy") || lowerText.contains("medicine")) {
            return "Healthcare";
        } else {
            return "Other";
        }
    }
    
    private String extractDescription(String text) {
        // Extract merchant/location - improved pattern
        Pattern pattern = Pattern.compile("(?:at|in|from|for)\\s+([A-Za-z\\s&'-]+?)(?:\\s+(?:today|yesterday|on|with|$))");
        Matcher matcher = pattern.matcher(text.toLowerCase());
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Try to extract merchant name patterns
        Pattern merchantPattern = Pattern.compile("(starbucks|mcdonalds|mcdonald's|subway|domino's|pizza hut|walmart|target|costco|amazon|uber|lyft)");
        Matcher merchantMatcher = merchantPattern.matcher(text.toLowerCase());
        if (merchantMatcher.find()) {
            return merchantMatcher.group(1).substring(0, 1).toUpperCase() + merchantMatcher.group(1).substring(1);
        }
        
        // Fallback to first part of text
        String[] words = text.split("\\s+");
        if (words.length > 3) {
            return String.join(" ", java.util.Arrays.copyOf(words, Math.min(4, words.length)));
        }
        
        return text;
    }
    
    private String extractPaymentMethod(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("credit card") || lowerText.contains("visa") || lowerText.contains("mastercard")) {
            return "Credit Card";
        } else if (lowerText.contains("debit card") || lowerText.contains("debit")) {
            return "Debit Card";
        } else if (lowerText.contains("cash") || lowerText.contains("paid cash")) {
            return "Cash";
        } else if (lowerText.contains("paypal") || lowerText.contains("venmo") || lowerText.contains("cash app")) {
            return "Digital Wallet";
        } else if (lowerText.contains("bank") || lowerText.contains("transfer")) {
            return "Bank Transfer";
        } else {
            return "Credit Card"; // Default
        }
    }
    
    public ExpenseDTO parseVoiceInput(String audioData, String format, User user) {
        // Process voice input and return ExpenseDTO
        String transcript = transcribeAudio(audioData);
        Map<String, Object> parsedData = parseExpenseFromText(transcript);
        
        return ExpenseDTO.builder()
                .description((String) parsedData.getOrDefault("description", "Voice Expense"))
                .amount(new BigDecimal(parsedData.getOrDefault("amount", "0").toString()))
                .category((String) parsedData.getOrDefault("category", "Other"))
                .expenseDate(LocalDate.now())
                .type("EXPENSE")
                .source("voice")
                .build();
    }
    
    public ExpenseDTO parseVoiceFile(org.springframework.web.multipart.MultipartFile audioFile, User user) {
        // Process voice file and return ExpenseDTO
        // Mock implementation - read file and process
        return ExpenseDTO.builder()
                .description("Voice File Expense")
                .amount(BigDecimal.ZERO)
                .category("Other")
                .expenseDate(LocalDate.now())
                .type("EXPENSE")
                .source("voice")
                .build();
    }
}
