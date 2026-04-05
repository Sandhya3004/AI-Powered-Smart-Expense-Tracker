package com.expense.controller;

import com.expense.dto.TransactionDTO;
import com.expense.entity.Expense;
import com.expense.service.EnhancedTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final EnhancedTransactionService transactionService;

    // Get all transactions with pagination and filtering
    @GetMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String amountRange,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        try {
            Pageable pageable = PageRequest.of(page, limit, 
                Sort.by(Sort.Direction.fromString(sortOrder), sortBy));
            
            Page<Expense> transactions = transactionService.getAllTransactions(
                search, category, dateRange, amountRange, pageable);
            
            return ResponseEntity.ok(Map.of(
                "transactions", transactions.getContent(),
                "currentPage", page,
                "totalPages", transactions.getTotalPages(),
                "totalElements", transactions.getTotalElements(),
                "hasNext", transactions.hasNext(),
                "hasPrevious", transactions.hasPrevious()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions: " + e.getMessage()));
        }
    }

    // Get transaction statistics
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> getTransactionStatistics(
            @RequestParam(defaultValue = "6months") String timeRange) {
        try {
            Map<String, Object> statistics = transactionService.getTransactionStatistics(timeRange);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch statistics: " + e.getMessage()));
        }
    }

    // Get transactions by category
    @GetMapping("/by-category")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<Map<String, Object>>> getTransactionsByCategory(
            @RequestParam(defaultValue = "6months") String timeRange) {
        try {
            List<Map<String, Object>> categoryData = transactionService.getTransactionsByCategory(timeRange);
            return ResponseEntity.ok(categoryData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get monthly trend data
    @GetMapping("/monthly-trend")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyTrend(
            @RequestParam(defaultValue = "6months") String timeRange) {
        try {
            List<Map<String, Object>> trendData = transactionService.getMonthlyTrend(timeRange);
            return ResponseEntity.ok(trendData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Export transactions
    @GetMapping("/export")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) {
        try {
            byte[] exportData = transactionService.exportTransactions(format);
            
            String filename = "transactions." + format.toLowerCase();
            String contentType = format.equalsIgnoreCase("csv") 
                    ? "text/csv" 
                    : "application/pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(exportData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get transactions for current month
    @GetMapping("/current-month")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> getCurrentMonthTransactions() {
        try {
            YearMonth currentMonth = YearMonth.now();
            LocalDate startDate = currentMonth.atDay(1);
            LocalDate endDate = currentMonth.atEndOfMonth();
            
            List<Expense> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "transactions", transactions,
                "month", currentMonth.toString(),
                "startDate", startDate,
                "endDate", endDate
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Create single transaction
    @PostMapping
    public ResponseEntity<Expense> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        Expense transaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    // Create bulk transactions
    @PostMapping("/bulk")
    public ResponseEntity<List<Expense>> createBulkTransactions(@Valid @RequestBody TransactionDTO transactionDTO) {
        List<Expense> transactions = transactionService.createBulkTransactions(transactionDTO.getTransactions());
        return ResponseEntity.ok(transactions);
    }

    // Create income transaction
    @PostMapping("/income")
    public ResponseEntity<Expense> createIncome(@Valid @RequestBody TransactionDTO transactionDTO) {
        transactionDTO.setType("INCOME");
        Expense transaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    // Create expense transaction
    @PostMapping("/expense")
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody TransactionDTO transactionDTO) {
        transactionDTO.setType("EXPENSE");
        Expense transaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    // Create transfer transaction
    @PostMapping("/transfer")
    public ResponseEntity<Expense> createTransfer(@Valid @RequestBody TransactionDTO transactionDTO) {
        Expense transaction = transactionService.createTransfer(transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    // Get all transactions with filtering
    @PostMapping("/search")
    public ResponseEntity<Page<Expense>> getTransactions(@Valid @RequestBody TransactionDTO filters) {
        Page<Expense> transactions = transactionService.getTransactions(filters);
        return ResponseEntity.ok(transactions);
    }

    // Get transactions by type
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<Expense>> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Expense> transactions = transactionService.getTransactionsByType(type, page, size);
        return ResponseEntity.ok(transactions);
    }

    // Get recent transactions
    @GetMapping("/recent")
    public ResponseEntity<List<Expense>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        List<Expense> transactions = transactionService.getRecentTransactions(limit);
        return ResponseEntity.ok(transactions);
    }

    // Get transactions by account
    @GetMapping("/account/{account}")
    public ResponseEntity<Page<Expense>> getTransactionsByAccount(
            @PathVariable String account,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Expense> transactions = transactionService.getTransactionsByAccount(account, page, size);
        return ResponseEntity.ok(transactions);
    }

    // Search transactions
    @GetMapping("/search/query")
    public ResponseEntity<Page<Expense>> searchTransactions(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Expense> transactions = transactionService.searchTransactions(query, page, size);
        return ResponseEntity.ok(transactions);
    }

    // Get transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getTransaction(@PathVariable Long id) {
        // This would need to be implemented in TransactionService
        // For now, returning a placeholder
        return ResponseEntity.ok().build();
    }

    // Update transaction
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDTO transactionDTO) {
        Expense transaction = transactionService.updateTransaction(id, transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    // Delete transaction
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }

    // Get transaction summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTransactionSummary(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        Map<String, Object> summary = transactionService.getTransactionSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    // Get recurring transactions
    @GetMapping("/recurring")
    public ResponseEntity<List<Expense>> getRecurringTransactions() {
        List<Expense> transactions = transactionService.getRecurringTransactions();
        return ResponseEntity.ok(transactions);
    }

    // Get monthly summary
    @GetMapping("/summary/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        Map<String, Object> summary = transactionService.getTransactionSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    // Get yearly summary
    @GetMapping("/summary/yearly")
    public ResponseEntity<Map<String, Object>> getYearlySummary(@RequestParam int year) {
        
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        Map<String, Object> summary = transactionService.getTransactionSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    // Duplicate transaction
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Expense> duplicateTransaction(@PathVariable Long id) {
        // This would need to be implemented in TransactionService
        // For now, returning a placeholder
        return ResponseEntity.ok().build();
    }

    // Mark transaction as recurring
    @PostMapping("/{id}/recurring")
    public ResponseEntity<Expense> markAsRecurring(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        TransactionDTO dto = TransactionDTO.builder()
                .isRecurring(true)
                .recurrencePattern(request.get("pattern"))
                .build();
        
        Expense transaction = transactionService.updateTransaction(id, dto);
        return ResponseEntity.ok(transaction);
    }

    // Get transactions by category
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<Expense>> getTransactionsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        TransactionDTO filters = TransactionDTO.builder()
                .category(category)
                .page(page)
                .size(size)
                .build();
        
        Page<Expense> transactions = transactionService.getTransactions(filters);
        return ResponseEntity.ok(transactions);
    }

    // Get transactions by date range
    @GetMapping("/date-range")
    public ResponseEntity<Page<Expense>> getTransactionsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        TransactionDTO filters = TransactionDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .build();
        
        Page<Expense> transactions = transactionService.getTransactions(filters);
        return ResponseEntity.ok(transactions);
    }
}
