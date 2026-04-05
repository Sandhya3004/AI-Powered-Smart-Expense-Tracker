package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.dto.ExpenseDTO;
import com.expense.dto.ExpenseResponseDTO;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.service.CSVService;
import com.expense.service.ExpenseService;
import com.expense.service.ReceiptOCRService;
import com.expense.service.VoiceInputService;
import com.expense.service.VoiceProcessingService;
import com.expense.util.CSVHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController extends BaseController {

    private final ExpenseService expenseService;
    private final VoiceInputService voiceInputService;
    private final CSVService csvService;
    private final VoiceProcessingService voiceProcessingService;
    private final ReceiptOCRService receiptOCRService;

    @PostMapping
    public ResponseEntity<?> createExpense(@Valid @RequestBody ExpenseDTO expenseDTO) {
        try {
            ExpenseResponseDTO expense = expenseService.createExpense(expenseDTO);
            return ResponseEntity.ok(ApiResponse.success(expense, "Expense created successfully"));
        } catch (Exception e) {
            log.error("Error creating expense", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create expense: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            log.info("Fetching expenses - page: {}, size: {}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<Expense> expensesPage = expenseService.getUserExpenses(pageable);
            
            // Map to DTOs to avoid LazyInitializationException
            List<ExpenseResponseDTO> expenseDTOs = expensesPage.getContent().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", expenseDTOs);
            response.put("totalElements", expensesPage.getTotalElements());
            response.put("totalPages", expensesPage.getTotalPages());
            response.put("number", expensesPage.getNumber());
            response.put("size", expensesPage.getSize());
            
            log.info("Successfully fetched {} expenses", expenseDTOs.size());
            return ResponseEntity.ok(ApiResponse.success(response, "Expenses retrieved successfully"));
        } catch (org.springframework.security.authentication.AuthenticationCredentialsNotFoundException e) {
            log.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required: " + e.getMessage()));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.error("Bad credentials: {}", e.getMessage());
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid credentials: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching expenses - Exception type: {}", e.getClass().getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch expenses: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExpenseById(@PathVariable Long id) {
        try {
            ExpenseResponseDTO expense = expenseService.getExpenseById(id);
            return ResponseEntity.ok(ApiResponse.success(expense, "Expense retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching expense by ID", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch expense: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseDTO expenseDTO) {
        try {
            ExpenseResponseDTO expense = expenseService.updateExpense(id, expenseDTO);
            return ResponseEntity.ok(ApiResponse.success(expense, "Expense updated successfully"));
        } catch (Exception e) {
            log.error("Error updating expense", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update expense: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        try {
            expenseService.deleteExpense(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting expense", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete expense: " + e.getMessage()));
        }
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        final Integer finalYear = year;
        final Integer finalMonth = month;
        
        if (finalYear == null || finalMonth == null) {
            // Default to current month
            java.time.LocalDate now = java.time.LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        try {
            Pageable pageable = PageRequest.of(0, 1000);
            Page<Expense> allExpenses = expenseService.getUserExpenses(pageable);
            
            // Filter expenses for the specified month/year
            java.util.List<Expense> monthlyExpenses = allExpenses.getContent().stream()
                    .filter(expense -> {
                        java.time.LocalDate expenseDate = expense.getExpenseDate();
                        return expenseDate.getYear() == finalYear && expenseDate.getMonthValue() == finalMonth;
                    })
                    .collect(java.util.stream.Collectors.toList());

            double totalExpenses = monthlyExpenses.stream()
                    .filter(expense -> "EXPENSE".equals(expense.getType()))
                    .mapToDouble(expense -> expense.getAmount().doubleValue())
                    .sum();

            double totalIncome = monthlyExpenses.stream()
                    .filter(expense -> "INCOME".equals(expense.getType()))
                    .mapToDouble(expense -> expense.getAmount().doubleValue())
                    .sum();

            double savings = totalIncome - totalExpenses;

            return ResponseEntity.ok(java.util.Map.of(
                "year", year,
                "month", month,
                "totalExpenses", totalExpenses,
                "totalIncome", totalIncome,
                "savings", savings,
                "transactionCount", monthlyExpenses.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", "Failed to fetch monthly summary: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<?> getCategoryBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        if (year == null || month == null) {
            // Default to current month
            java.time.LocalDate now = java.time.LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        try {
            Pageable pageable = PageRequest.of(0, 1000);
            Page<Expense> allExpenses = expenseService.getUserExpenses(pageable);
            
            // Filter expenses for the specified month/year
            final int finalYear = year != null ? year : java.time.LocalDate.now().getYear();
            final int finalMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();
            
            final java.util.List<Expense> monthlyExpenses = allExpenses.getContent().stream()
                    .filter(expense -> {
                        java.time.LocalDate expenseDate = expense.getExpenseDate();
                        return expenseDate.getYear() == finalYear && expenseDate.getMonthValue() == finalMonth;
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Group by category and sum amounts
            Map<String, Double> categoryTotals = monthlyExpenses.stream()
                    .filter(expense -> "EXPENSE".equals(expense.getType()))
                    .collect(java.util.stream.Collectors.groupingBy(
                            Expense::getCategory,
                            java.util.stream.Collectors.summingDouble(expense -> expense.getAmount().doubleValue())
                    ));

            return ResponseEntity.ok(Map.of(
                "year", year,
                "month", month,
                "categoryBreakdown", categoryTotals,
                "totalExpenses", categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch category breakdown: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/upload-receipt")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> uploadReceipt(@RequestParam("receipt") MultipartFile receipt) {
        try {
            log.info("Uploading receipt: {}", receipt.getOriginalFilename());
            
            // Validate file type
            if (!isValidImageFile(receipt)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid file type. Please upload JPEG or PNG image."));
            }
            
            // Validate file size (max 5MB)
            if (receipt.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size too large. Maximum size is 5MB."));
            }
            
            // Process receipt using OCR service
            var expenseDTO = receiptOCRService.processReceipt(receipt);
            ExpenseResponseDTO expense = expenseService.createExpense(expenseDTO);
            
            return ResponseEntity.ok(ApiResponse.success(expense, 
                String.format("Receipt processed: %s - %s", expense.getDescription(), expense.getCategory())));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid receipt: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading receipt", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to process receipt: " + e.getMessage()));
        }
    }

    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> processVoiceText(@RequestBody Map<String, String> request) {
        try {
            log.info("Processing voice text input");
            
            String text = request.get("text");
            
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Text is required. Example: 'spent 500 on food'"));
            }
            
            // Use the new VoiceProcessingService to parse text
            var expenseDTO = voiceProcessingService.parseVoiceText(text);
            ExpenseResponseDTO expense = expenseService.createExpense(expenseDTO);
            
            return ResponseEntity.ok(ApiResponse.success(expense, 
                String.format("Voice expense added: %s - %s", expense.getDescription(), expense.getCategory())));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid voice input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing voice input", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to process voice input: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadCSV(@RequestParam("file") MultipartFile file) {
        try {
            User currentUser = getCurrentUser();
            log.info("Uploading CSV file for user: {}", currentUser.getId());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No file provided"));
            }
            
            // Parse CSV file
            List<Expense> expenses = csvService.parseCSV(file, currentUser);
            
            Map<String, Object> result = Map.of(
                "totalRows", expenses.size(),
                "imported", expenses.size(),
                "failed", 0,
                "errors", List.of()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result, "CSV import completed"));
            
        } catch (Exception e) {
            log.error("CSV upload failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to process CSV: " + e.getMessage()));
        }
    }

    @PostMapping("/import-csv")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importCSV(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Importing CSV file: {}", file.getOriginalFilename());
            
            // Validate CSV file
            if (!CSVHelper.isValidCSVFile(file)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid CSV file. Please upload a .csv file."));
            }
            
            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size too large. Maximum size is 10MB."));
            }
            
            CSVHelper.ImportResult result = expenseService.importCSV(file);
            
            Map<String, Object> summary = Map.of(
                "totalRows", result.getTotalRows(),
                "successful", result.getSuccessful(),
                "failed", result.getFailed(),
                "errors", result.getErrors()
            );
            
            return ResponseEntity.ok(ApiResponse.success(summary, "CSV import completed"));
            
        } catch (Exception e) {
            log.error("Error importing CSV", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to import CSV: " + e.getMessage()));
        }
    }

    private boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/webp")
        );
    }

    private boolean isValidAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.equals("audio/wav") ||
            contentType.equals("audio/mp3") ||
            contentType.equals("audio/mpeg") ||
            contentType.equals("audio/mp4") ||
            contentType.equals("audio/ogg") ||
            contentType.equals("audio/webm")
        );
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
}