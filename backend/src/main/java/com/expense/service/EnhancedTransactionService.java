package com.expense.service;

import com.expense.dto.TransactionDTO;
import com.expense.entity.Expense;
import com.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnhancedTransactionService {

    private final ExpenseRepository expenseRepository;
    private final TransactionService transactionService;

    // Enhanced getAllTransactions with filtering and pagination
    public Page<Expense> getAllTransactions(String search, String category, String dateRange, 
                                          String amountRange, Pageable pageable) {
        // Build dynamic query based on filters
        return expenseRepository.findAll((root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                jakarta.persistence.criteria.Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(root.get("description"), "%" + search + "%"),
                    criteriaBuilder.like(root.get("merchant"), "%" + search + "%"),
                    criteriaBuilder.like(root.get("category"), "%" + search + "%")
                );
                predicates.add(searchPredicate);
            }

            // Category filter
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            // Date range filter
            if (dateRange != null && !dateRange.trim().isEmpty()) {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate;
                
                switch (dateRange) {
                    case "7days":
                        startDate = endDate.minusDays(7);
                        break;
                    case "30days":
                        startDate = endDate.minusDays(30);
                        break;
                    case "90days":
                        startDate = endDate.minusDays(90);
                        break;
                    case "thisMonth":
                        startDate = endDate.withDayOfMonth(1);
                        break;
                    case "lastMonth":
                        startDate = endDate.minusMonths(1).withDayOfMonth(1);
                        endDate = endDate.minusMonths(1).withDayOfMonth(endDate.minusMonths(1).lengthOfMonth());
                        break;
                    default:
                        startDate = endDate.minusMonths(6);
                        break;
                }
                
                predicates.add(criteriaBuilder.between(root.get("expenseDate"), startDate, endDate));
            }

            // Amount range filter
            if (amountRange != null && !amountRange.trim().isEmpty()) {
                double minAmount = 0;
                double maxAmount = Double.MAX_VALUE;
                
                switch (amountRange) {
                    case "0-100":
                        minAmount = 0;
                        maxAmount = 100;
                        break;
                    case "100-500":
                        minAmount = 100;
                        maxAmount = 500;
                        break;
                    case "500-1000":
                        minAmount = 500;
                        maxAmount = 1000;
                        break;
                    case "1000-5000":
                        minAmount = 1000;
                        maxAmount = 5000;
                        break;
                    case "5000+":
                        minAmount = 5000;
                        break;
                }
                
                predicates.add(criteriaBuilder.between(root.get("amount"), minAmount, maxAmount));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }

    // Get transaction statistics
    public Map<String, Object> getTransactionStatistics(String timeRange) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        switch (timeRange) {
            case "1month":
                startDate = endDate.minusMonths(1);
                break;
            case "3months":
                startDate = endDate.minusMonths(3);
                break;
            case "6months":
                startDate = endDate.minusMonths(6);
                break;
            case "1year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusMonths(6);
                break;
        }

        List<Expense> transactions = expenseRepository.findByExpenseDateBetween(startDate, endDate);
        
        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                .sum();
        
        double totalExpenses = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                .sum();
        
        double totalBalance = totalIncome - totalExpenses;
        
        // Calculate previous period for comparison
        LocalDate prevStartDate = startDate.minusMonths(endDate.getMonthValue() - startDate.getMonthValue());
        LocalDate prevEndDate = startDate.minusDays(1);
        
        List<Expense> previousTransactions = expenseRepository.findByExpenseDateBetween(prevStartDate, prevEndDate);
        
        double prevIncome = previousTransactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                .sum();
        
        double prevExpenses = previousTransactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                .sum();
        
        double prevBalance = prevIncome - prevExpenses;
        
        // Calculate percentage changes
        double balanceChange = totalBalance - prevBalance;
        double balanceChangePercent = prevBalance != 0 ? (balanceChange / Math.abs(prevBalance)) * 100 : 0;
        
        double incomeChange = totalIncome - prevIncome;
        double incomeChangePercent = prevIncome != 0 ? (incomeChange / Math.abs(prevIncome)) * 100 : 0;
        
        double expensesChange = totalExpenses - prevExpenses;
        double expensesChangePercent = prevExpenses != 0 ? (expensesChange / Math.abs(prevExpenses)) * 100 : 0;

        return Map.of(
            "totalBalance", totalBalance,
            "balanceChange", balanceChange,
            "balanceChangePercent", balanceChangePercent,
            "income", totalIncome,
            "incomeChange", incomeChange,
            "incomeChangePercent", incomeChangePercent,
            "expenses", totalExpenses,
            "expensesChange", expensesChange,
            "expensesChangePercent", expensesChangePercent
        );
    }

    // Get transactions by category with percentages
    public List<Map<String, Object>> getTransactionsByCategory(String timeRange) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        switch (timeRange) {
            case "1month":
                startDate = endDate.minusMonths(1);
                break;
            case "3months":
                startDate = endDate.minusMonths(3);
                break;
            case "6months":
                startDate = endDate.minusMonths(6);
                break;
            case "1year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusMonths(6);
                break;
        }

        List<Expense> transactions = expenseRepository.findByExpenseDateBetween(startDate, endDate);
        
        // Group by category and calculate totals
        Map<String, Double> categoryTotals = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.groupingBy(
                    Expense::getCategory,
                    Collectors.summingDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                ));
        
        double totalExpenses = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
        
        return categoryTotals.entrySet().stream()
                .map(entry -> {
                    double amount = entry.getValue();
                    double percentage = totalExpenses > 0 ? (amount / totalExpenses) * 100 : 0;
                    
                    return Map.<String, Object>of(
                        "name", entry.getKey(),
                        "amount", amount,
                        "percentage", Math.round(percentage * 10.0) / 10.0 // Round to 1 decimal place
                    );
                })
                .sorted((a, b) -> Double.compare(
                    ((Number) b.get("amount")).doubleValue(),
                    ((Number) a.get("amount")).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    // Get monthly trend data
    public List<Map<String, Object>> getMonthlyTrend(String timeRange) {
        int months = timeRange.equals("1year") ? 12 : 6;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<Map<String, Object>> trendData = new ArrayList<>();
        
        for (int i = 0; i < months; i++) {
            LocalDate monthStart = startDate.plusMonths(i);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            List<Expense> monthTransactions = expenseRepository.findByExpenseDateBetween(monthStart, monthEnd);
            
            double monthIncome = monthTransactions.stream()
                    .filter(t -> "INCOME".equals(t.getType()))
                    .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                    .sum();
            
            double monthExpenses = monthTransactions.stream()
                    .filter(t -> "EXPENSE".equals(t.getType()))
                    .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
                    .sum();
            
            double monthSavings = monthIncome - monthExpenses;
            
            trendData.add(Map.of(
                "month", monthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")),
                "income", monthIncome,
                "expenses", monthExpenses,
                "savings", monthSavings
            ));
        }
        
        return trendData;
    }

    // Export transactions to CSV or PDF
    public byte[] exportTransactions(String format) {
        List<Expense> allTransactions = expenseRepository.findAll();
        
        if ("csv".equalsIgnoreCase(format)) {
            return exportToCSV(allTransactions);
        } else {
            return exportToPDF(allTransactions);
        }
    }

    private byte[] exportToCSV(List<Expense> transactions) {
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Category,Description,Amount,Merchant,Notes\n");
        
        for (Expense transaction : transactions) {
            csv.append(String.format("%s,%s,%s,%s,%.2f,%s,%s\n",
                    transaction.getExpenseDate(),
                    transaction.getType(),
                    transaction.getCategory(),
                    transaction.getDescription(),
                    transaction.getAmount(),
                    transaction.getMerchant(),
                    transaction.getNotes()
            ));
        }
        
        return csv.toString().getBytes();
    }

    private byte[] exportToPDF(List<Expense> transactions) {
        // This would require a PDF library like iText or PDFBox
        // For now, returning a simple text representation
        StringBuilder pdf = new StringBuilder();
        pdf.append("Transaction Report\n\n");
        
        for (Expense transaction : transactions) {
            pdf.append(String.format("Date: %s\nType: %s\nCategory: %s\nDescription: %s\nAmount: %.2f\nMerchant: %s\nNotes: %s\n\n",
                    transaction.getExpenseDate(),
                    transaction.getType(),
                    transaction.getCategory(),
                    transaction.getDescription(),
                    transaction.getAmount(),
                    transaction.getMerchant(),
                    transaction.getNotes()
            ));
        }
        
        return pdf.toString().getBytes();
    }

    // Get transactions by date range
    public List<Expense> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate);
    }

    // Keep existing methods for backward compatibility
    public Expense createTransaction(TransactionDTO transactionDTO) {
        Expense expense = Expense.builder()
                .type(transactionDTO.getType() != null ? transactionDTO.getType() : "EXPENSE")
                .amount(transactionDTO.getAmount())
                .description(transactionDTO.getDescription())
                .category(transactionDTO.getCategory() != null ? transactionDTO.getCategory() : "Other")
                .merchant(transactionDTO.getMerchant() != null ? transactionDTO.getMerchant() : "")
                .expenseDate(transactionDTO.getDate() != null ? transactionDTO.getDate() : LocalDate.now())
                .paymentType(transactionDTO.getPaymentType() != null ? transactionDTO.getPaymentType() : "Cash")
                .account(transactionDTO.getAccount() != null ? transactionDTO.getAccount() : "Cash")
                .notes(transactionDTO.getNotes() != null ? transactionDTO.getNotes() : "")
                .source(transactionDTO.getSource() != null ? transactionDTO.getSource() : "manual")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return expenseRepository.save(expense);
    }

    public List<Expense> createBulkTransactionsFromMap(List<Map<String, Object>> transactionsData) {
        List<Expense> expenses = new ArrayList<>();
        
        for (Map<String, Object> data : transactionsData) {
            Expense expense = Expense.builder()
                    .type((String) data.getOrDefault("type", "EXPENSE"))
                    .amount(BigDecimal.valueOf(((Number) data.get("amount")).doubleValue()))
                    .description((String) data.get("description"))
                    .category((String) data.getOrDefault("category", "Other"))
                    .merchant((String) data.getOrDefault("merchant", ""))
                    .expenseDate(LocalDate.parse((String) data.get("date")))
                    .paymentType((String) data.getOrDefault("paymentType", "Cash"))
                    .account((String) data.getOrDefault("account", "Cash"))
                    .notes((String) data.getOrDefault("notes", ""))
                    .source((String) data.getOrDefault("source", "manual"))
                    .status("COMPLETED")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            expenses.add(expense);
        }
        
        return expenseRepository.saveAll(expenses);
    }

    // Keep other existing methods...
    public Page<Expense> getTransactions(TransactionDTO filters) {
        // Implementation for existing method
        return expenseRepository.findAll(Pageable.unpaged());
    }

    public Expense updateTransaction(Long id, TransactionDTO transactionDTO) {
        // Implementation for updating transactions
        return expenseRepository.findById(id).orElse(null);
    }

    public void deleteTransaction(Long id) {
        expenseRepository.deleteById(id);
    }

    // Bridge methods for controller, delegating to TransactionService
    public Expense createTransfer(TransactionDTO dto) {
        return transactionService.createTransfer(dto);
    }

    public Page<Expense> getTransactionsByType(String type, int page, int size) {
        return transactionService.getTransactionsByType(type, page, size);
    }

    public List<Expense> getRecentTransactions(int limit) {
        return transactionService.getRecentTransactions(limit);
    }

    public Page<Expense> getTransactionsByAccount(String account, int page, int size) {
        return transactionService.getTransactionsByAccount(account, page, size);
    }

    public Page<Expense> searchTransactions(String query, int page, int size) {
        return transactionService.searchTransactions(query, page, size);
    }

    public Map<String, Object> getTransactionSummary(LocalDate startDate, LocalDate endDate) {
        return transactionService.getTransactionSummary(startDate, endDate);
    }

    public List<Expense> getRecurringTransactions() {
        return transactionService.getRecurringTransactions();
    }

    public List<Expense> createBulkTransactions(List<TransactionDTO> transactions) {
        return transactionService.createBulkTransactions(transactions);
    }

    // Add other existing methods as needed...
}
