package com.expense.service;

import com.expense.dto.TransactionDTO;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    // Create single transaction
    public Expense createTransaction(TransactionDTO dto) {
        User user = authService.getCurrentUser();
        
        Expense transaction = Expense.builder()
                .user(user)
                .amount(dto.getAmount())
                .type(dto.getType())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .merchant(dto.getMerchant())
                .paymentType(dto.getPaymentType())
                .account(dto.getAccount())
                .expenseDate(dto.getDate())
                .source(dto.getSource() != null ? dto.getSource() : "manual")
                .notes(dto.getNotes())
                .tags(dto.getTags())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurrencePattern(dto.getRecurrencePattern())
                .status(dto.getStatus() != null ? dto.getStatus() : "COMPLETED")
                .fromAccount(dto.getFromAccount())
                .toAccount(dto.getToAccount())
                .build();

        return expenseRepository.save(transaction);
    }

    // Create bulk transactions
    public List<Expense> createBulkTransactions(List<TransactionDTO> transactions) {
        User user = authService.getCurrentUser();
        List<Expense> expenses = new ArrayList<>();

        for (TransactionDTO dto : transactions) {
            Expense expense = Expense.builder()
                    .user(user)
                    .amount(dto.getAmount())
                    .type(dto.getType())
                    .description(dto.getDescription())
                    .category(dto.getCategory())
                    .merchant(dto.getMerchant())
                    .paymentType(dto.getPaymentType())
                    .account(dto.getAccount())
                    .expenseDate(dto.getDate())
                    .source(dto.getSource() != null ? dto.getSource() : "manual")
                    .notes(dto.getNotes())
                    .tags(dto.getTags())
                    .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                    .recurrencePattern(dto.getRecurrencePattern())
                    .status(dto.getStatus() != null ? dto.getStatus() : "COMPLETED")
                    .fromAccount(dto.getFromAccount())
                    .toAccount(dto.getToAccount())
                    .build();
            expenses.add(expense);
        }

        return expenseRepository.saveAll(expenses);
    }

    // Get transactions with pagination and filtering
    public Page<Expense> getTransactions(TransactionDTO filters) {
        User user = authService.getCurrentUser();
        
        // Build sort
        Sort sort = Sort.by(
            filters.getSortDirection().equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC,
            filters.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize(), sort);

        // Apply filters
        if (filters.getSearch() != null && !filters.getSearch().trim().isEmpty()) {
            return expenseRepository.findByUserAndSearch(user, filters.getSearch().trim(), pageable);
        }

        if (filters.getStartDate() != null || filters.getEndDate() != null || 
            filters.getType() != null || filters.getCategory() != null || 
            filters.getAccount() != null || filters.getMinAmount() != null || 
            filters.getMaxAmount() != null) {
            
            LocalDate startDate = filters.getStartDate() != null ? filters.getStartDate() : LocalDate.of(2000, 1, 1);
            LocalDate endDate = filters.getEndDate() != null ? filters.getEndDate() : LocalDate.now();
            
            return expenseRepository.findByUserWithFilters(
                user, startDate, endDate, filters.getType(), 
                filters.getCategory(), filters.getAccount(), 
                filters.getMinAmount(), filters.getMaxAmount(), pageable
            );
        }

        return expenseRepository.findByUser(user, pageable);
    }

    // Get transactions by type
    public Page<Expense> getTransactionsByType(String type, int page, int size) {
        User user = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate"));
        return expenseRepository.findByUserAndType(user, type, pageable);
    }

    // Get recent transactions
    public List<Expense> getRecentTransactions(int limit) {
        User user = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "expenseDate", "createdAt"));
        return expenseRepository.findRecentTransactions(user, pageable);
    }

    // Update transaction
    public Expense updateTransaction(Long id, TransactionDTO dto) {
        User user = authService.getCurrentUser();
        
        Expense transaction = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Verify ownership
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDescription(dto.getDescription());
        transaction.setCategory(dto.getCategory());
        transaction.setMerchant(dto.getMerchant());
        transaction.setPaymentType(dto.getPaymentType());
        transaction.setAccount(dto.getAccount());
        transaction.setExpenseDate(dto.getDate());
        transaction.setNotes(dto.getNotes());
        transaction.setTags(dto.getTags());
        transaction.setIsRecurring(dto.getIsRecurring());
        transaction.setRecurrencePattern(dto.getRecurrencePattern());
        transaction.setStatus(dto.getStatus());
        transaction.setFromAccount(dto.getFromAccount());
        transaction.setToAccount(dto.getToAccount());

        return expenseRepository.save(transaction);
    }

    // Delete transaction
    public void deleteTransaction(Long id) {
        User user = authService.getCurrentUser();
        
        Expense transaction = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Verify ownership
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        expenseRepository.delete(transaction);
    }

    // Get transaction summary
    public Map<String, Object> getTransactionSummary(LocalDate startDate, LocalDate endDate) {
        User user = authService.getCurrentUser();
        
        BigDecimal totalIncome = expenseRepository.getTotalIncomeByDateRange(user, startDate, endDate);
        BigDecimal totalExpenses = expenseRepository.getTotalExpensesByDateRange(user, startDate, endDate);
        BigDecimal netAmount = totalIncome.subtract(totalExpenses);

        // Category breakdown
        List<Object[]> categoryBreakdown = expenseRepository.getCategoryBreakdown(user, startDate, endDate);
        List<Map<String, Object>> categories = categoryBreakdown.stream()
                .map(row -> Map.of(
                    "category", row[0],
                    "amount", row[1]
                ))
                .collect(Collectors.toList());

        // Account balances
        List<Object[]> accountBalances = expenseRepository.getAccountBalance(user, startDate, endDate);
        List<Map<String, Object>> accounts = accountBalances.stream()
                .map(row -> Map.of(
                    "account", row[0],
                    "balance", row[1]
                ))
                .collect(Collectors.toList());

        return Map.of(
            "totalIncome", totalIncome,
            "totalExpenses", totalExpenses,
            "netAmount", netAmount,
            "categoryBreakdown", categories,
            "accountBalances", accounts
        );
    }

    // Get recurring transactions
    public List<Expense> getRecurringTransactions() {
        User user = authService.getCurrentUser();
        return expenseRepository.findByUserAndIsRecurringTrue(user);
    }

    // Create transfer transaction
    public Expense createTransfer(TransactionDTO dto) {
        dto.setType("TRANSFER");
        dto.setStatus("COMPLETED");
        return createTransaction(dto);
    }

    // Get transactions by account
    public Page<Expense> getTransactionsByAccount(String account, int page, int size) {
        User user = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate"));
        return expenseRepository.findByUserAndAccount(user, account, pageable);
    }

    // Search transactions
    public Page<Expense> searchTransactions(String query, int page, int size) {
        User user = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate"));
        return expenseRepository.findByUserAndSearch(user, query, pageable);
    }
}
