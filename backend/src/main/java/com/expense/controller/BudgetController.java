package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.dto.BudgetDTO;
import com.expense.entity.Budget;
import com.expense.entity.User;
import com.expense.exception.ResourceNotFoundException;
import com.expense.service.BudgetService;
import com.expense.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController extends BaseController {

    private final BudgetService budgetService;
    private final UserService userService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Budget>> getCurrentBudget() {
        try {
            log.info("Getting current budget for user: {}", getCurrentUser().getId());
            Budget budget = budgetService.getCurrentMonthBudget(getCurrentUser());
            log.info("Current budget retrieved successfully: {}", budget.getId());
            return ResponseEntity.ok(ApiResponse.success(budget, "Current budget retrieved"));
        } catch (Exception e) {
            log.error("Error getting current budget: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get current budget: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Budget>> setBudget(@RequestBody @Valid BudgetDTO budgetDTO) {
        try {
            String month = budgetDTO.getMonth() == null ? YearMonth.now().toString() : budgetDTO.getMonth();
            User user = getCurrentUser();
            
            // Check if budget already exists
            Budget existingBudget = budgetService.findByUserAndCategoryAndMonth(user, budgetDTO.getCategory(), month);
            
            Budget savedBudget;
            if (existingBudget != null) {
                // Update existing budget
                savedBudget = budgetService.setBudget(budgetDTO, user);
                return ResponseEntity.ok(ApiResponse.success(savedBudget, "Budget updated successfully"));
            } else {
                // Create new budget
                savedBudget = budgetService.setBudget(budgetDTO, user);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(savedBudget, "Budget created successfully"));
            }
        } catch (Exception e) {
            log.error("Error setting budget: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to set budget: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Budget>> updateBudget(@PathVariable Long id, @RequestBody @Valid BudgetDTO budgetDTO) {
        try {
            log.info("Updating budget with id: {} for user: {}", id, getCurrentUser().getId());
            Budget updatedBudget = budgetService.updateBudget(id, budgetDTO, getCurrentUser());
            log.info("Budget updated successfully: {}", updatedBudget.getId());
            return ResponseEntity.ok(ApiResponse.success(updatedBudget, "Budget updated successfully"));
        } catch (ResourceNotFoundException e) {
            log.warn("Budget not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Budget not found: " + e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Access denied for budget update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating budget: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update budget: " + e.getMessage()));
        }
    }
}