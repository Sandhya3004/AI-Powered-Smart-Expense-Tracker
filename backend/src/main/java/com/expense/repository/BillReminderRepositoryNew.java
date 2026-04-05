package com.expense.repository;

import com.expense.entity.BillReminder;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Repository
public interface BillReminderRepositoryNew extends JpaRepository<BillReminder, Long> {

    List<BillReminder> findByUserOrderByDueDateAsc(User user);

    List<BillReminder> findByUserAndDueDateBetweenOrderByDueDateAsc(User user, LocalDate startDate, LocalDate endDate);

    List<BillReminder> findByUserAndIsPaidFalse(User user);

    List<BillReminder> findByUserAndDueDateBeforeAndIsPaidFalse(User user, LocalDate date);

    @Query("SELECT b FROM BillReminder b WHERE b.user.id = :userId AND MONTH(b.dueDate) = :month AND YEAR(b.dueDate) = :year")
    List<BillReminder> findByUserAndMonthAndYear(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BillReminder b WHERE b.user.id = :userId AND b.isPaid = false AND b.recurrencePattern IS NOT NULL")
    BigDecimal sumUnpaidRecurringBills(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM BillReminder b WHERE b.user.id = :userId AND b.isPaid = false AND b.dueDate < :date")
    long countOverdueBills(@Param("userId") Long userId, @Param("date") LocalDate date);
}
