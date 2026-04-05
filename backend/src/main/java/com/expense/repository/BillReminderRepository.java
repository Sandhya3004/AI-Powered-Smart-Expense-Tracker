package com.expense.repository;

import com.expense.entity.BillReminder;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillReminderRepository extends JpaRepository<BillReminder, Long> {
    
    List<BillReminder> findByUser(User user);
    
    List<BillReminder> findByUserOrderByDueDateAsc(User user);
    
    List<BillReminder> findByUserAndDueDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    List<BillReminder> findByUserAndDueDateBeforeAndIsPaidFalse(User user, LocalDate date);
    
    List<BillReminder> findByStatus(String status);
    
    @Query("SELECT br FROM BillReminder br WHERE br.user = :user AND br.dueDate >= :startDate AND br.dueDate <= :endDate ORDER BY br.dueDate ASC")
    List<BillReminder> findByUserAndDueDateBetweenOrderByDueDateAsc(@Param("user") User user, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT br FROM BillReminder br WHERE br.user = :user AND br.status = :status ORDER BY br.dueDate ASC")
    List<BillReminder> findByUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    @Query("SELECT COUNT(br) FROM BillReminder br WHERE br.user = :user AND br.isPaid = false AND br.dueDate < :currentDate")
    long countOverdueBills(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT br FROM BillReminder br WHERE br.user = :user AND br.isPaid = false AND br.dueDate BETWEEN :startDate AND :endDate ORDER BY br.dueDate ASC")
    List<BillReminder> findUpcomingBills(@Param("user") User user, 
                                         @Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);
}
