package com.expense.repository;

import com.expense.entity.Group;
import com.expense.entity.GroupExpense;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, Long> {
    
    List<GroupExpense> findByGroup(Group group);
    
    List<GroupExpense> findByParticipantsContaining(User user);
    
    List<GroupExpense> findByCreatedByOrderByCreatedAtDesc(User user);
    
    @Query("SELECT ge FROM GroupExpense ge WHERE :user MEMBER OF ge.participants ORDER BY ge.createdAt DESC")
    List<GroupExpense> findByUserAsParticipant(@Param("user") User user);
    
    @Query("SELECT ge FROM GroupExpense ge WHERE ge.createdBy = :user ORDER BY ge.createdAt DESC")
    List<GroupExpense> findByUserAsCreator(@Param("user") User user);
    
    @Query("SELECT ge FROM GroupExpense ge WHERE :user MEMBER OF ge.participants AND ge.status = :status")
    List<GroupExpense> findByUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    @Query("SELECT ge FROM GroupExpense ge WHERE :user MEMBER OF ge.participants AND ge.createdAt >= :startDate")
    List<GroupExpense> findByUserAndCreatedAfter(@Param("user") User user, @Param("startDate") java.time.LocalDateTime startDate);
    
    @Query("SELECT COUNT(ge) FROM GroupExpense ge WHERE :user MEMBER OF ge.participants AND ge.status = 'ACTIVE'")
    long countActiveGroupExpenses(@Param("user") User user);
    
    @Query("SELECT ge FROM GroupExpense ge WHERE ge.createdBy = :user AND ge.status = :status")
    List<GroupExpense> findByCreatorAndStatus(@Param("user") User user, @Param("status") String status);
}
