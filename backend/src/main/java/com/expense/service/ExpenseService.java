package com.expense.service;

import com.expense.dto.*;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.exception.ResourceNotFoundException;
import com.expense.repository.ExpenseRepository;
import com.expense.repository.UserRepository;
import com.expense.util.CSVHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);
    
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AIServiceClient aiServiceClient;

    public ExpenseResponseDTO createExpense(ExpenseDTO dto) {
        // Get current user from the security context
        User user = getCurrentUserFromSecurityContext();

        Expense expense = Expense.builder()
                .user(user)
                .amount(dto.getAmount())
                .type(dto.getType() != null ? dto.getType() : "EXPENSE")
                .description(dto.getDescription())
                .category(dto.getCategory() != null ? dto.getCategory() : "Other")
                .merchant(dto.getMerchant())
                .paymentType(dto.getPaymentType() != null ? dto.getPaymentType() : "Cash")
                .account(dto.getAccount() != null ? dto.getAccount() : "Cash")
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .expenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : java.time.LocalDate.now())
                .source(dto.getSource() != null ? dto.getSource() : "manual")
                .notes(dto.getNotes())
                .tags(dto.getTags())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurrencePattern(dto.getRecurrencePattern())
                .status(dto.getStatus() != null ? dto.getStatus() : "COMPLETED")
                .fromAccount(dto.getFromAccount())
                .toAccount(dto.getToAccount())
                .build();
        Expense saved = expenseRepository.save(expense);
        return mapToDTO(saved);
    }

    private User getCurrentUserFromSecurityContext() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new AuthenticationCredentialsNotFoundException("User not authenticated");
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            } else if (principal instanceof String) {
                String email = (String) principal;
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new BadCredentialsException("User not found: " + email));
            } else {
                String email = principal.toString();
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new BadCredentialsException("User not found: " + email));
            }
        } catch (AuthenticationCredentialsNotFoundException | BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting current user from security context", e);
            throw new AuthenticationCredentialsNotFoundException("Authentication error: " + e.getMessage());
        }
    }

    public Page<Expense> getUserExpenses(Pageable pageable) {
        try {
            User user = getCurrentUserFromSecurityContext();
            if (user == null) {
                log.error("User is null after authentication check");
                throw new AuthenticationCredentialsNotFoundException("User not authenticated");
            }
            
            // Reload user from database to ensure it's a managed entity
            User managedUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new BadCredentialsException("User not found in database: " + user.getId()));
            
            return expenseRepository.findByUser(managedUser, pageable);
        } catch (AuthenticationCredentialsNotFoundException | BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user expenses", e);
            throw new AuthenticationCredentialsNotFoundException("Failed to authenticate user: " + e.getMessage());
        }
    }

    public ExpenseResponseDTO getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getUser().getId().equals(getCurrentUserFromSecurityContext().getId())) {
            throw new SecurityException("Access denied");
        }
        return mapToDTO(expense);
    }

    public ExpenseResponseDTO updateExpense(Long id, ExpenseDTO dto) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getUser().getId().equals(getCurrentUserFromSecurityContext().getId())) {
            throw new SecurityException("Access denied");
        }
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setCategory(dto.getCategory());
        expense.setMerchant(dto.getMerchant());
        expense.setPaymentType(dto.getPaymentType());
        expense.setExpenseDate(dto.getExpenseDate());
        Expense updated = expenseRepository.save(expense);
        return mapToDTO(updated);
    }

    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getUser().getId().equals(getCurrentUserFromSecurityContext().getId())) {
            throw new SecurityException("Access denied");
        }
        expenseRepository.delete(expense);
    }

    private ExpenseResponseDTO mapToDTO(Expense expense) {
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .type(expense.getType())
                .description(expense.getDescription())
                .category(expense.getCategory())
                .merchant(expense.getMerchant())
                .paymentType(expense.getPaymentType())
                .account(expense.getAccount())
                .currency(expense.getCurrency())
                .expenseDate(expense.getExpenseDate())
                .source(expense.getSource())
                .notes(expense.getNotes())
                .tags(expense.getTags())
                .isRecurring(expense.getIsRecurring())
                .recurrencePattern(expense.getRecurrencePattern())
                .status(expense.getStatus())
                .fromAccount(expense.getFromAccount())
                .toAccount(expense.getToAccount())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    public CSVHelper.ImportResult importCSV(MultipartFile file) {
        User user = getCurrentUserFromSecurityContext();
        
        if (!CSVHelper.isValidCSVFile(file)) {
            return new CSVHelper.ImportResult(0, 0, 0, List.of("Invalid CSV file"), List.of());
        }

        CSVHelper.ImportResult result = CSVHelper.parseCSV(file, user);
        
        // Save all valid expenses
        List<Expense> savedExpenses = expenseRepository.saveAll(result.getExpenses());
        
        log.info("CSV import completed: {} total, {} successful, {} failed", 
                result.getTotalRows(), result.getSuccessful(), result.getFailed());
        
        return result;
    }

    public ExpenseResponseDTO createExpenseFromReceipt(MultipartFile file) {
        User user = getCurrentUserFromSecurityContext();
        
        try {
            log.info("Processing receipt upload for user: {}", user.getId());
            
            // Send to AI service for OCR processing
            Map<String, Object> ocrResult = aiServiceClient.processReceipt(file);
            
            if (ocrResult.containsKey("success") && Boolean.TRUE.equals(ocrResult.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> extractedData = (Map<String, Object>) ocrResult.get("extracted_data");
                
                // Create expense from extracted data
                Expense expense = Expense.builder()
                        .user(user)
                        .description(extractedData.containsKey("description") ? 
                                extractedData.get("description").toString() : "Receipt Purchase")
                        .amount(new BigDecimal(extractedData.get("total_amount").toString()))
                        .category(extractedData.containsKey("category") ? 
                                extractedData.get("category").toString() : "Other")
                        .merchant(extractedData.containsKey("merchant") ? 
                                extractedData.get("merchant").toString() : "Unknown")
                        .expenseDate(extractedData.containsKey("date") ? 
                                java.time.LocalDate.parse(extractedData.get("date").toString()) : 
                                java.time.LocalDate.now())
                        .source("RECEIPT")
                        .type("EXPENSE")
                        .currency("INR")
                        .paymentType("Cash")
                        .account("Cash")
                        .notes("Auto-generated from receipt OCR")
                        .build();
                
                Expense saved = expenseRepository.save(expense);
                log.info("Expense created from receipt: {}", saved.getId());
                return mapToDTO(saved);
            } else {
                String error = ocrResult.containsKey("error") ? ocrResult.get("error").toString() : "OCR processing failed";
                throw new RuntimeException("Failed to process receipt: " + error);
            }
        } catch (Exception e) {
            log.error("Error processing receipt", e);
            throw new RuntimeException("Failed to process receipt: " + e.getMessage());
        }
    }

    public ExpenseResponseDTO createExpenseFromVoice(String audioData, String format) {
        User user = getCurrentUserFromSecurityContext();
        
        try {
            log.info("Processing voice input for user: {}", user.getId());
            
            // Send to AI service for voice processing (base64 endpoint)
            Map<String, Object> voiceResult = aiServiceClient.processVoice(audioData, format);
            
            if (voiceResult.containsKey("success") && Boolean.TRUE.equals(voiceResult.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) voiceResult.get("data");
                
                // Create expense from voice data
                Expense expense = Expense.builder()
                        .user(user)
                        .description(data.containsKey("description") ? 
                                data.get("description").toString() : "Voice Expense")
                        .amount(data.containsKey("amount") ? 
                                new BigDecimal(data.get("amount").toString()) : BigDecimal.ZERO)
                        .category(data.containsKey("category") ? 
                                data.get("category").toString() : "Other")
                        .expenseDate(data.containsKey("expenseDate") ? 
                                java.time.LocalDate.parse(data.get("expenseDate").toString()) : 
                                java.time.LocalDate.now())
                        .source("VOICE")
                        .type("EXPENSE")
                        .currency("INR")
                        .paymentType("Cash")
                        .account("Cash")
                        .notes("Auto-generated from voice input")
                        .build();
                
                Expense saved = expenseRepository.save(expense);
                log.info("Expense created from voice: {}", saved.getId());
                return mapToDTO(saved);
            } else {
                String error = voiceResult.containsKey("error") ? voiceResult.get("error").toString() : "Voice processing failed";
                throw new RuntimeException("Failed to process voice: " + error);
            }
        } catch (Exception e) {
            log.error("Error processing voice input", e);
            throw new RuntimeException("Failed to process voice input: " + e.getMessage());
        }
    }

    public ExpenseResponseDTO createExpenseFromVoiceFile(MultipartFile audioFile) {
        User user = getCurrentUserFromSecurityContext();
        
        try {
            log.info("Processing voice file for user: {}", user.getId());
            
            // Send to AI service for voice processing (file endpoint)
            Map<String, Object> voiceResult = aiServiceClient.processVoiceFile(audioFile);
            
            if (voiceResult.containsKey("success") && Boolean.TRUE.equals(voiceResult.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) voiceResult.get("data");
                
                // Create expense from voice data
                Expense expense = Expense.builder()
                        .user(user)
                        .description(data.containsKey("description") ? 
                                data.get("description").toString() : "Voice Expense")
                        .amount(data.containsKey("amount") ? 
                                new BigDecimal(data.get("amount").toString()) : BigDecimal.ZERO)
                        .category(data.containsKey("category") ? 
                                data.get("category").toString() : "Other")
                        .expenseDate(data.containsKey("expenseDate") ? 
                                java.time.LocalDate.parse(data.get("expenseDate").toString()) : 
                                java.time.LocalDate.now())
                        .source("VOICE")
                        .type("EXPENSE")
                        .currency("INR")
                        .paymentType("Cash")
                        .account("Cash")
                        .notes("Auto-generated from voice file")
                        .build();
                
                Expense saved = expenseRepository.save(expense);
                log.info("Expense created from voice file: {}", saved.getId());
                return mapToDTO(saved);
            } else {
                String error = voiceResult.containsKey("error") ? voiceResult.get("error").toString() : "Voice processing failed";
                throw new RuntimeException("Failed to process voice file: " + error);
            }
        } catch (Exception e) {
            log.error("Error processing voice file", e);
            throw new RuntimeException("Failed to process voice file: " + e.getMessage());
        }
    }

    public String processCsv(MultipartFile file) {
        log.info("Processing CSV file: {}", file.getOriginalFilename());
        return "CSV processing not yet implemented";
    }

    public String processReceiptOcr(MultipartFile file) {
        log.info("Processing receipt OCR: {}", file.getOriginalFilename());
        return "Receipt OCR not yet implemented";
    }

    public String processSms(String smsText) {
        log.info("Processing SMS: {}", smsText);
        return "SMS processing not yet implemented";
    }

    // Additional methods needed for dashboard and insights
    public BigDecimal getTotalExpensesByDateRange(User user, LocalDateTime start, LocalDateTime end) {
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                user, start.toLocalDate(), end.toLocalDate());
        return expenses.stream()
                .filter(expense -> "EXPENSE".equals(expense.getType()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalExpensesByMonth(User user, YearMonth month) {
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                user, month.atDay(1), month.atEndOfMonth());
        return expenses.stream()
                .filter(expense -> "EXPENSE".equals(expense.getType()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalIncomeByMonth(User user, YearMonth month) {
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                user, month.atDay(1), month.atEndOfMonth());
        return expenses.stream()
                .filter(expense -> "INCOME".equals(expense.getType()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalByCategoryForMonth(User user, String category, YearMonth month) {
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                user, month.atDay(1), month.atEndOfMonth());
        return expenses.stream()
                .filter(expense -> "EXPENSE".equals(expense.getType()))
                .filter(expense -> category.equals(expense.getCategory()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<BigDecimal> getMonthlyTotals(User user, int months) {
        List<BigDecimal> totals = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal total = getTotalExpensesByMonth(user, month);
            totals.add(total);
        }
        return totals;
    }

    public List<Map<String, Object>> getCategoryBreakdown(User user, YearMonth month) {
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetween(
                user, month.atDay(1), month.atEndOfMonth());
        
        // Filter only expenses (not income) and group by category
        Map<String, BigDecimal> categoryTotals = expenses.stream()
                .filter(expense -> "EXPENSE".equals(expense.getType()))
                .collect(Collectors.groupingBy(
                    Expense::getCategory,
                    Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
        
        // Convert to list of maps with percentage calculation
        BigDecimal totalExpenses = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Map<String, Object>> categoryData = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            Map<String, Object> categoryInfo = new HashMap<>();
            categoryInfo.put("category", entry.getKey());
            categoryInfo.put("amount", entry.getValue());
            
            // Calculate percentage
            double percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0 
                    ? entry.getValue().doubleValue() / totalExpenses.doubleValue() * 100 
                    : 0.0;
            categoryInfo.put("percentage", Math.round(percentage * 10.0) / 10.0);
            
            categoryData.add(categoryInfo);
        }
        
        // Sort by amount descending
        categoryData.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        
        return categoryData;
    }
}