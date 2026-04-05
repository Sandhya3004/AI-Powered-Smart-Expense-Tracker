package com.expense.service;

import com.expense.dto.BudgetDTO;
import com.expense.entity.Budget;
import com.expense.entity.User;
import com.expense.exception.ResourceNotFoundException;
import com.expense.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public Budget getCurrentMonthBudget(User user) {
        String month = YearMonth.now().toString();
        List<Budget> budgets = budgetRepository.findByUserAndMonthOrderByCategory(user, month);
        
        if (budgets.isEmpty()) {
            // Return a default budget with 0 amount for current month
            Budget defaultBudget = Budget.builder()
                    .user(user)
                    .month(month)
                    .category("General")  // Default category
                    .totalBudget(BigDecimal.ZERO)
                    .build();
            return budgetRepository.save(defaultBudget);
        }
        
        // If multiple budgets exist, return the first one (or you could aggregate)
        return budgets.get(0);
    }

    public Budget setBudget(BudgetDTO dto, User user) {
        String month = dto.getMonth() == null ? YearMonth.now().toString() : dto.getMonth();
        
        // Check if budget already exists for this user, category, and month
        Budget existingBudget = findByUserAndCategoryAndMonth(user, dto.getCategory(), month);
        
        if (existingBudget != null) {
            // Update existing budget
            existingBudget.setTotalBudget(dto.getTotalBudget());
            return budgetRepository.save(existingBudget);
        } else {
            // Create new budget
            Budget budget = Budget.builder()
                    .user(user)
                    .category(dto.getCategory())
                    .month(month)
                    .totalBudget(dto.getTotalBudget())
                    .build();
            return budgetRepository.save(budget);
        }
    }

    public Budget findByUserAndCategoryAndMonth(User user, String category, String month) {
        return budgetRepository.findByUserAndCategoryAndMonth(user, category, month).orElse(null);
    }

    public Budget updateBudget(Long id, BudgetDTO dto, User user) {
        log.info("Updating budget {} for user {}", id, user.getId());
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        
        // Debug: Print user IDs for comparison
        log.debug("Budget user ID: {}", budget.getUser().getId());
        log.debug("Current user ID: {}", user.getId());
        log.debug("Budget user ID class: {}", budget.getUser().getId().getClass().getName());
        log.debug("Current user ID class: {}", user.getId().getClass().getName());
        log.debug("Are IDs equal?: {}", budget.getUser().getId().equals(user.getId()));
        
        // Verify the budget belongs to the current user
        if (!budget.getUser().getId().equals(user.getId())) {
            log.warn("Access denied - budget {} belongs to user {}, but requested by user {}", 
                    id, budget.getUser().getId(), user.getId());
            throw new SecurityException("Access denied: Budget does not belong to current user");
        }
        
        // Update fields
        if (dto.getTotalBudget() != null) {
            budget.setTotalBudget(dto.getTotalBudget());
        }
        if (dto.getCategory() != null) {
            budget.setCategory(dto.getCategory());
        }
        if (dto.getMonth() != null) {
            budget.setMonth(dto.getMonth());
        }
        
        Budget saved = budgetRepository.save(budget);
        log.info("Budget {} updated successfully", saved.getId());
        return saved;
    }
}