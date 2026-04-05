package com.expense.service;

import com.expense.dto.ExpenseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class VoiceProcessingService {

    // Pattern to match amounts (handles various formats)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:rs\\.?|rupees?|\\₹|\\$|eur|euros?|\\€)?\\s*" +  // Optional currency symbol/word
        "(\\d+(?:[.,]\\d{1,2})?)" +                          // The amount (e.g., 500, 500.50, 1,234.56)
        "\\s*(?:rs\\.?|rupees?|\\₹)?",                       // Optional trailing currency
        Pattern.CASE_INSENSITIVE
    );

    // Category keywords for rule-based classification
    private static final String[][] CATEGORY_KEYWORDS = {
        {"Food & Dining", "food,foodie,dining,restaurant,swiggy,zomato,uber eats,lunch,dinner,breakfast,meal,pizza,burger,sushi,cafe,coffee,tea,snack,groceries,grocery,supermarket,bigbasket,bblink,jiomart"},
        {"Transportation", "transport,uber,ola,rapido,auto,taxi,cab,train,bus,metro,flight,airplane,travel,petrol,diesel,fuel,vehicle,car,bike,scooter,rickshaw"},
        {"Shopping", "shopping,amazon,flipkart,myntra,ajio,meesho,clothes,clothing,fashion,apparel,shoes,sneakers,electronics,gadgets,phone,laptop,accessories,jewelry,watch"},
        {"Entertainment", "entertainment,movie,cinema,theatre,netflix,prime,disney+,hotstar,spotify,concert,game,gaming,event,ticket,sports,hobby,fun,party"},
        {"Bills & Utilities", "bill,electricity,water,gas,internet,broadband,wifi,mobile,recharge,utility,maintenance,rent,emi,loan,insurance,tax,government"},
        {"Healthcare", "health,medical,doctor,hospital,clinic,pharmacy,medicine,medicines,pills,drugs,healthcare,dental,vision,therapy,consultation,checkup"},
        {"Education", "education,school,college,university,course,class,tuition,fees,book,books,stationery,exam,learning,certification,training,workshop"},
        {"Personal Care", "personal care,salon,spa,gym,fitness,haircut,beard,shave,cosmetics,beauty,skincare,makeup,toiletries"},
        {"Gifts & Donations", "gift,present,donation,charity,wedding,birthday,anniversary,celebration,festival,diwali,christmas"},
        {"Travel", "travel,trip,vacation,hotel,hostel,airbnb,booking,goa,manali,shimla,tourism,sightseeing,resort,stay"},
        {"Investments", "investment,stocks,shares,mutual fund,sip,fd,rd,bank,deposit,crypto,bitcoin,trading,broker"},
        {"Other", "other,miscellaneous,general"}
    };

    /**
     * Parse voice/text input to extract expense details
     * Expected formats:
     * - "spent 500 on food"
     * - "paid 1000 rupees for taxi"
     * - "bought groceries for 250"
     * - "300 rs lunch"
     * - "uber ride 180"
     */
    public ExpenseDTO parseVoiceText(String text) {
        log.info("Parsing voice text: {}", text);
        
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Voice text cannot be empty");
        }
        
        String normalizedText = text.toLowerCase().trim();
        
        // Extract amount
        BigDecimal amount = extractAmount(normalizedText);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Could not detect amount in voice input. Please specify an amount (e.g., 'spent 500 on food')");
        }
        
        // Extract description and category
        String description = extractDescription(normalizedText, amount);
        String category = categorizeExpense(normalizedText, description);
        
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(amount);
        dto.setDescription(description);
        dto.setCategory(category);
        dto.setExpenseDate(LocalDate.now());
        dto.setType("EXPENSE");
        dto.setSource("VOICE");
        dto.setCurrency("INR");
        dto.setPaymentType("Cash");
        dto.setStatus("COMPLETED");
        
        log.info("Parsed expense: amount={}, description={}, category={}", 
            amount, description, category);
        
        return dto;
    }

    private BigDecimal extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        BigDecimal foundAmount = null;
        
        while (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replace(",", "");
                BigDecimal amount = new BigDecimal(amountStr);
                // Keep the largest amount found (in case of multiple numbers)
                if (foundAmount == null || amount.compareTo(foundAmount) > 0) {
                    foundAmount = amount;
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse amount: {}", matcher.group(1));
            }
        }
        
        return foundAmount;
    }

    private String extractDescription(String text, BigDecimal amount) {
        // Remove the amount from text to get description
        String description = text;
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            description = text.replace(matcher.group(0), "").trim();
        }
        
        // Clean up common words
        description = description
            .replaceAll("(?:spent|paid|bought|purchased|gave|transferred|sent)\\s*", "")
            .replaceAll("(?:for|on|at|in|to)\\s*", "")
            .replaceAll("(?:rs\\.?|rupees?|\\₹|\\$)", "")
            .trim();
        
        // If description is empty after cleanup, use the category or a generic description
        if (description.isEmpty()) {
            description = "Voice expense";
        }
        
        // Capitalize first letter
        return description.substring(0, 1).toUpperCase() + description.substring(1);
    }

    private String categorizeExpense(String text, String description) {
        String combinedText = text + " " + description;
        String lowerText = combinedText.toLowerCase();
        
        for (String[] categoryData : CATEGORY_KEYWORDS) {
            String category = categoryData[0];
            String keywords = categoryData[1];
            
            for (String keyword : keywords.split(",")) {
                if (lowerText.contains(keyword.trim())) {
                    return category;
                }
            }
        }
        
        return "Other";
    }
}
