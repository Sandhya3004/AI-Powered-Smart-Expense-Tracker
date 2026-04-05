package com.expense.repository;

import com.expense.entity.Bill;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByUser(User user);

    List<Bill> findByUserOrderByDueDateAsc(User user);

    List<Bill> findByUserAndDueDateBetweenOrderByDueDateAsc(User user, LocalDate startDate, LocalDate endDate);

    List<Bill> findByUserAndIsPaidFalse(User user);

    List<Bill> findByUserAndDueDateBeforeAndIsPaidFalse(User user, LocalDate date);

    @Query("SELECT b FROM Bill b WHERE b.user.id = :userId AND MONTH(b.dueDate) = :month AND YEAR(b.dueDate) = :year")
    List<Bill> findByUserAndMonthAndYear(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bill b WHERE b.user.id = :userId AND b.isPaid = false AND b.isRecurring = true")
    BigDecimal sumUnpaidRecurringBills(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.user.id = :userId AND b.isPaid = false AND b.dueDate < :date")
    long countOverdueBills(@Param("userId") Long userId, @Param("date") LocalDate date);
}
