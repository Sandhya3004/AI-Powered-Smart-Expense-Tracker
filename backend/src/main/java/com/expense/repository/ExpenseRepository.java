package com.expense.repository;

import com.expense.entity.Expense;
import com.expense.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    
    Page<Expense> findByUser(User user, Pageable pageable);
    
    List<Expense> findByUserAndExpenseDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Expense> findByUserAndExpenseDateBetweenAndType(User user, LocalDate start, LocalDate end, String type);
    List<Expense> findByUserAndExpenseDateBetweenAndCategory(User user, LocalDate start, LocalDate end, String category);

    // For enhanced service (global range queries)
    List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);

    // Search and filter queries
    @Query("SELECT e FROM Expense e WHERE e.user = :user AND " +
           "(LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.merchant) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Expense> findByUserAndSearch(@Param("user") User user, @Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.user = :user AND " +
           "e.expenseDate BETWEEN :startDate AND :endDate AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:account IS NULL OR e.account = :account) AND " +
           "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR e.amount <= :maxAmount)")
    Page<Expense> findByUserWithFilters(@Param("user") User user,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("type") String type,
                                       @Param("category") String category,
                                       @Param("account") String account,
                                       @Param("minAmount") BigDecimal minAmount,
                                       @Param("maxAmount") BigDecimal maxAmount,
                                       Pageable pageable);

    // Aggregation queries
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.type = 'EXPENSE' AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal getTotalExpensesByDateRange(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.type = 'INCOME' AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal getTotalIncomeByDateRange(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e.category, SUM(e.amount) as total FROM Expense e WHERE e.user = :user AND e.type = 'EXPENSE' AND e.expenseDate BETWEEN :start AND :end GROUP BY e.category ORDER BY total DESC")
    List<Object[]> getCategoryBreakdown(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e.account, SUM(CASE WHEN e.type = 'EXPENSE' THEN -e.amount ELSE e.amount END) as net FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :start AND :end GROUP BY e.account")
    List<Object[]> getAccountBalance(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // Recurring transactions
    List<Expense> findByUserAndIsRecurringTrue(User user);
    List<Expense> findByUserAndIsRecurringTrueAndRecurrencePattern(User user, String pattern);

    // Recent transactions
    @Query("SELECT e FROM Expense e WHERE e.user = :user ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findRecentTransactions(@Param("user") User user, Pageable pageable);

    // Added for AI service – query by userId directly
    List<Expense> findByUserIdAndExpenseDateBetween(Long userId, LocalDate start, LocalDate end);

    // Transfer queries
    List<Expense> findByUserAndTypeAndFromAccount(User user, String type, String fromAccount);
    List<Expense> findByUserAndTypeAndToAccount(User user, String type, String toAccount);
    
    // Additional helper methods
    List<Expense> findByUserOrderByExpenseDateDesc(User user);
    List<Expense> findTop10ByUserOrderByExpenseDateDesc(User user);
    
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND EXTRACT(YEAR FROM e.expenseDate) = :year AND EXTRACT(MONTH FROM e.expenseDate) = :month GROUP BY e.category")
    List<Object[]> findCategoryTotalsForMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
    
    @Query("SELECT EXTRACT(YEAR FROM e.expenseDate), EXTRACT(MONTH FROM e.expenseDate), SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.type = 'EXPENSE' GROUP BY EXTRACT(YEAR FROM e.expenseDate), EXTRACT(MONTH FROM e.expenseDate) ORDER BY EXTRACT(YEAR FROM e.expenseDate), EXTRACT(MONTH FROM e.expenseDate)")
    List<Object[]> getMonthlyTotals(@Param("user") User user);
    
    // Additional methods for TransactionService
    Page<Expense> findByUserAndType(User user, String type, Pageable pageable);
    Page<Expense> findByUserAndAccount(User user, String account, Pageable pageable);
    
    // For DashboardService - need List return type
    List<Expense> findByUserAndType(User user, String type);
}