package com.expense.repository;

import com.expense.entity.SavingsGoal;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserOrderByTargetDateAsc(User user);

    List<SavingsGoal> findByUserAndIsCompletedFalseOrderByTargetDateAsc(User user);
}
