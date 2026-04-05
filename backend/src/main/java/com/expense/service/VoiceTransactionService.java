package com.expense.service;

import com.expense.dto.TransactionDTO;
import com.expense.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceTransactionService {

    private final TransactionService transactionService;
    private final AIExpenseCategorizationService categorizationService;
    private final AuthService authService;

    // Voice command patterns
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:Rs|rupees?|₹)?\\s*([0-9,]+(?:\\.[0-9]*)?)");
    private static final Pattern MERCHANT_PATTERN = Pattern.compile("(?:at|from|in)\\s+([a-zA-Z][a-zA-Z\\s&']*[a-zA-Z])");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("(?:for|category)\\s+([a-zA-Z][a-zA-Z\\s]*[a-zA-Z])");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:on|date)\\s+([0-9]{1,2}[\\/-][0-9]{1,2}[\\/-][0-9]{2,4})");
    private static final Pattern PAYMENT_TYPE_PATTERN = Pattern.compile("(?:paid by|via|using)\\s+([a-zA-Z][a-zA-Z\\s]*[a-zA-Z])");
    
    // Common voice commands
    private static final Set<String> EXPENSE_COMMANDS = Set.of(
        "spent", "paid", "bought", "purchased", "expense", "cost", "charged"
    );
    
    private static final Set<String> INCOME_COMMANDS = Set.of(
        "received", "earned", "got", "income", "salary", "bonus", "payment received"
    );
    
    private static final Set<String> TRANSFER_COMMANDS = Set.of(
        "transferred", "moved", "sent", "transfer", "from account to"
    );

    /**
     * Process voice command and create transaction
     */
    public Map<String, Object> processVoiceCommand(String voiceText) {
        try {
            User user = authService.getCurrentUser();
            
            // Normalize voice text
            String normalizedText = normalizeVoiceText(voiceText);
            
            // Parse transaction details
            TransactionDTO transaction = parseVoiceCommand(normalizedText);
            
            if (transaction == null) {
                return Map.of(
                    "success", false,
                    "message", "Could not understand the voice command. Please try again."
                );
            }
            
            // Set default values
            transaction.setSource("voice");
            transaction.setStatus("COMPLETED");
            transaction.setDate(transaction.getDate() != null ? transaction.getDate() : LocalDate.now());
            
            // Auto-categorize if not provided
            if (transaction.getCategory() == null || transaction.getCategory().isEmpty()) {
                String suggestedCategory = categorizationService.categorizeExpense(
                    transaction.getDescription(),
                    transaction.getMerchant(),
                    transaction.getAmount()
                );
                transaction.setCategory(suggestedCategory);
            }
            
            // Create transaction
            com.expense.entity.Expense createdTransaction = transactionService.createTransaction(transaction);
            
            return Map.of(
                "success", true,
                "message", "Transaction created successfully",
                "transaction", createdTransaction,
                "parsedData", Map.of(
                    "amount", transaction.getAmount(),
                    "type", transaction.getType(),
                    "merchant", transaction.getMerchant(),
                    "category", transaction.getCategory()
                )
            );
            
        } catch (Exception e) {
            log.error("Error processing voice command", e);
            return Map.of(
                "success", false,
                "message", "Error processing voice command: " + e.getMessage()
            );
        }
    }

    /**
     * Parse voice command into transaction DTO
     */
    private TransactionDTO parseVoiceCommand(String text) {
        TransactionDTO dto = new TransactionDTO();
        
        // Determine transaction type
        String type = determineTransactionType(text);
        dto.setType(type);
        
        // Extract amount
        BigDecimal amount = extractAmount(text);
        if (amount == null) {
            return null;
        }
        dto.setAmount(amount);
        
        // Extract merchant/description
        String merchant = extractMerchant(text);
        if (merchant != null) {
            dto.setMerchant(merchant);
            dto.setDescription("Purchase from " + merchant);
        } else {
            dto.setDescription("Voice transaction");
        }
        
        // Extract category
        String category = extractCategory(text);
        if (category != null) {
            dto.setCategory(category);
        }
        
        // Extract date
        LocalDate date = extractDate(text);
        if (date != null) {
            dto.setDate(date);
        }
        
        // Extract payment type
        String paymentType = extractPaymentType(text);
        if (paymentType != null) {
            dto.setPaymentType(paymentType);
        }
        
        // Handle transfers
        if (type.equals("TRANSFER")) {
            extractTransferDetails(text, dto);
        }
        
        return dto;
    }

    /**
     * Normalize voice text for processing
     */
    private String normalizeVoiceText(String text) {
        if (text == null) return "";
        
        return text.toLowerCase()
                .replaceAll("rupees", "rs")
                .replaceAll("₹", "rs")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Determine transaction type from voice command
     */
    private String determineTransactionType(String text) {
        for (String command : EXPENSE_COMMANDS) {
            if (text.contains(command)) {
                return "EXPENSE";
            }
        }
        
        for (String command : INCOME_COMMANDS) {
            if (text.contains(command)) {
                return "INCOME";
            }
        }
        
        for (String command : TRANSFER_COMMANDS) {
            if (text.contains(command)) {
                return "TRANSFER";
            }
        }
        
        // Default to expense for safety
        return "EXPENSE";
    }

    /**
     * Extract amount from voice text
     */
    private BigDecimal extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            String amountStr = matcher.group(1).replace(",", "");
            try {
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                log.warn("Could not parse amount: {}", amountStr);
            }
        }
        return null;
    }

    /**
     * Extract merchant from voice text
     */
    private String extractMerchant(String text) {
        Matcher matcher = MERCHANT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extract category from voice text
     */
    private String extractCategory(String text) {
        Matcher matcher = CATEGORY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extract date from voice text
     */
    private LocalDate extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException e2) {
                    log.warn("Could not parse date: {}", dateStr);
                }
            }
        }
        return null;
    }

    /**
     * Extract payment type from voice text
     */
    private String extractPaymentType(String text) {
        Matcher matcher = PAYMENT_TYPE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extract transfer details for transfer transactions
     */
    private void extractTransferDetails(String text, TransactionDTO dto) {
        // Simple pattern for "from X to Y"
        Pattern transferPattern = Pattern.compile("from\\s+([a-zA-Z][a-zA-Z\\s]*[a-zA-Z])\\s+to\\s+([a-zA-Z][a-zA-Z\\s]*[a-zA-Z])");
        Matcher matcher = transferPattern.matcher(text);
        
        if (matcher.find()) {
            dto.setFromAccount(matcher.group(1).trim());
            dto.setToAccount(matcher.group(2).trim());
        }
    }

    /**
     * Get voice command examples for user guidance
     */
    public Map<String, Object> getVoiceCommandExamples() {
        return Map.of(
            "expense_examples", Arrays.asList(
                "Spent Rs 500 at restaurant for lunch",
                "Paid Rs 2000 for electricity bill",
                "Bought groceries for Rs 1500 at supermarket",
                "Charged Rs 300 on credit card for fuel"
            ),
            "income_examples", Arrays.asList(
                "Received Rs 50000 salary",
                "Got Rs 2000 bonus from company",
                "Earned Rs 10000 from freelance work",
                "Payment received Rs 5000 from client"
            ),
            "transfer_examples", Arrays.asList(
                "Transferred Rs 10000 from savings to current account",
                "Moved Rs 5000 from bank account to digital wallet",
                "Sent Rs 2000 from credit card to bank account"
            ),
            "advanced_examples", Arrays.asList(
                "Spent Rs 800 at cafe coffee day on credit card yesterday",
                "Paid Rs 1500 for electricity bill via net banking on 15/03/2024",
                "Bought groceries for Rs 2500 at big bazaar using debit card for food category"
            )
        );
    }

    /**
     * Validate voice command before processing
     */
    public Map<String, Object> validateVoiceCommand(String voiceText) {
        if (voiceText == null || voiceText.trim().isEmpty()) {
            return Map.of(
                "valid", false,
                "message", "Voice command is empty"
            );
        }

        String normalizedText = normalizeVoiceText(voiceText);
        
        // Check if it contains a transaction command
        boolean hasCommand = EXPENSE_COMMANDS.stream().anyMatch(normalizedText::contains) ||
                           INCOME_COMMANDS.stream().anyMatch(normalizedText::contains) ||
                           TRANSFER_COMMANDS.stream().anyMatch(normalizedText::contains);

        if (!hasCommand) {
            return Map.of(
                "valid", false,
                "message", "No transaction command found. Use words like 'spent', 'paid', 'received', etc."
            );
        }

        // Check if it contains an amount
        BigDecimal amount = extractAmount(normalizedText);
        if (amount == null) {
            return Map.of(
                "valid", false,
                "message", "No amount found. Please specify an amount like 'Rs 500'"
            );
        }

        return Map.of(
            "valid", true,
            "message", "Voice command is valid"
        );
    }

    /**
     * Get voice command suggestions based on partial input
     */
    public List<String> getVoiceCommandSuggestions(String partialText) {
        if (partialText == null) return Collections.emptyList();
        
        String normalizedPartial = normalizeVoiceText(partialText);
        List<String> suggestions = new ArrayList<>();

        // Amount suggestions
        if (normalizedPartial.contains("rs") || normalizedPartial.contains("spent")) {
            suggestions.add("Spent Rs [amount] at [merchant]");
        }

        // Merchant suggestions
        if (normalizedPartial.contains("at")) {
            suggestions.add("at restaurant, at supermarket, at petrol pump");
        }

        // Category suggestions
        if (normalizedPartial.contains("for")) {
            suggestions.add("for food, for transport, for shopping, for bills");
        }

        return suggestions.stream().distinct().limit(5).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
