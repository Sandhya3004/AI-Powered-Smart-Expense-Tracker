package com.expense.service;

import com.expense.entity.SavingsGoal;
import com.expense.entity.User;
import com.expense.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;

    public List<SavingsGoal> getUserSavingsGoals(User user) {
        try {
            return savingsGoalRepository.findByUserOrderByTargetDateAsc(user);
        } catch (Exception e) {
            log.error("Error fetching user savings goals", e);
            return List.of();
        }
    }

    public List<SavingsGoal> getActiveGoals(User user) {
        try {
            return savingsGoalRepository.findByUserAndIsCompletedFalseOrderByTargetDateAsc(user);
        } catch (Exception e) {
            log.error("Error fetching active savings goals", e);
            return List.of();
        }
    }

    public SavingsGoal createSavingsGoal(SavingsGoal goal) {
        try {
            goal.setCreatedAt(LocalDateTime.now());
            goal.setUpdatedAt(LocalDateTime.now());
            goal.setCurrentAmount(BigDecimal.ZERO);
            goal.setCompleted(false);
            return savingsGoalRepository.save(goal);
        } catch (Exception e) {
            log.error("Error creating savings goal", e);
            throw new RuntimeException("Failed to create savings goal: " + e.getMessage(), e);
        }
    }

    public SavingsGoal updateSavingsGoal(SavingsGoal goal) {
        try {
            goal.setUpdatedAt(LocalDateTime.now());
            return savingsGoalRepository.save(goal);
        } catch (Exception e) {
            log.error("Error updating savings goal", e);
            throw new RuntimeException("Failed to update savings goal: " + e.getMessage(), e);
        }
    }

    public SavingsGoal getSavingsGoalById(Long id) {
        try {
            return savingsGoalRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("Error fetching savings goal by id", e);
            return null;
        }
    }

    public void deleteSavingsGoal(Long id) {
        try {
            savingsGoalRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error deleting savings goal", e);
            throw new RuntimeException("Failed to delete savings goal: " + e.getMessage(), e);
        }
    }

    public SavingsGoal contributeToGoal(Long id, BigDecimal amount) {
        try {
            Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(id);
            if (goalOpt.isEmpty()) {
                throw new RuntimeException("Savings goal not found");
            }
            
            SavingsGoal goal = goalOpt.get();
            BigDecimal newCurrentAmount = goal.getCurrentAmount().add(amount);
            
            // Check if goal is completed
            if (newCurrentAmount.compareTo(goal.getTargetAmount()) >= 0) {
                goal.setCurrentAmount(goal.getTargetAmount());
                goal.setCompleted(true);
            } else {
                goal.setCurrentAmount(newCurrentAmount);
            }
            
            goal.setUpdatedAt(LocalDateTime.now());
            return savingsGoalRepository.save(goal);
        } catch (Exception e) {
            log.error("Error contributing to savings goal", e);
            throw new RuntimeException("Failed to add contribution: " + e.getMessage(), e);
        }
    }
}
