package com.expense.repository;

import com.expense.entity.Budget;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserAndMonth(User user, String month);
    
    List<Budget> findByUserAndMonthOrderByCategory(User user, String month);
    
    List<Budget> findByUser(User user);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.category = :category AND b.month = :month")
    Optional<Budget> findByUserAndCategoryAndMonth(@Param("user") User user, @Param("category") String category, @Param("month") String month);
}