package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.SavingsGoal;
import com.expense.entity.User;
import com.expense.service.SavingsGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
@Slf4j
public class SavingsGoalController extends BaseController {

    private final SavingsGoalService savingsGoalService;

    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<List<SavingsGoal>>> getAllGoals() {
        try {
            User user = getCurrentUser();
            List<SavingsGoal> goals = savingsGoalService.getUserSavingsGoals(user);
            return ResponseEntity.ok(ApiResponse.success(goals, "Savings goals retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching savings goals", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch savings goals: " + e.getMessage()));
        }
    }

    @GetMapping("/goals/active")
    public ResponseEntity<ApiResponse<List<SavingsGoal>>> getActiveGoals() {
        try {
            User user = getCurrentUser();
            List<SavingsGoal> activeGoals = savingsGoalService.getActiveGoals(user);
            return ResponseEntity.ok(ApiResponse.success(activeGoals, "Active savings goals retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching active savings goals", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch active savings goals: " + e.getMessage()));
        }
    }

    @PostMapping("/goals")
    public ResponseEntity<ApiResponse<SavingsGoal>> createGoal(@RequestBody SavingsGoal goal) {
        try {
            User user = getCurrentUser();
            goal.setUser(user);
            SavingsGoal createdGoal = savingsGoalService.createSavingsGoal(goal);
            return ResponseEntity.ok(ApiResponse.success(createdGoal, "Savings goal created successfully"));
        } catch (Exception e) {
            log.error("Error creating savings goal", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create savings goal: " + e.getMessage()));
        }
    }

    @PutMapping("/goals/{id}")
    public ResponseEntity<ApiResponse<SavingsGoal>> updateGoal(@PathVariable Long id, @RequestBody SavingsGoal goal) {
        try {
            User user = getCurrentUser();
            SavingsGoal existingGoal = savingsGoalService.getSavingsGoalById(id);
            
            if (existingGoal == null || !existingGoal.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Savings goal not found or access denied"));
            }
            
            goal.setId(id);
            goal.setUser(user);
            SavingsGoal updatedGoal = savingsGoalService.updateSavingsGoal(goal);
            return ResponseEntity.ok(ApiResponse.success(updatedGoal, "Savings goal updated successfully"));
        } catch (Exception e) {
            log.error("Error updating savings goal", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update savings goal: " + e.getMessage()));
        }
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            SavingsGoal existingGoal = savingsGoalService.getSavingsGoalById(id);
            
            if (existingGoal == null || !existingGoal.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Savings goal not found or access denied"));
            }
            
            savingsGoalService.deleteSavingsGoal(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Savings goal deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting savings goal", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete savings goal: " + e.getMessage()));
        }
    }

    @PostMapping("/goals/{id}/contribute")
    public ResponseEntity<ApiResponse<SavingsGoal>> contributeToGoal(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User user = getCurrentUser();
            SavingsGoal existingGoal = savingsGoalService.getSavingsGoalById(id);
            
            if (existingGoal == null || !existingGoal.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Savings goal not found or access denied"));
            }
            
            BigDecimal contributionAmount = new BigDecimal(request.get("amount").toString());
            SavingsGoal updatedGoal = savingsGoalService.contributeToGoal(id, contributionAmount);
            return ResponseEntity.ok(ApiResponse.success(updatedGoal, "Contribution added successfully"));
        } catch (Exception e) {
            log.error("Error contributing to savings goal", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to add contribution: " + e.getMessage()));
        }
    }
}
