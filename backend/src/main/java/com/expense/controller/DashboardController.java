package com.expense.controller;

import com.expense.dto.*;
import com.expense.entity.User;
import com.expense.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        try {
            User currentUser = getCurrentUser();
            DashboardResponse data = dashboardService.getDashboardResponse(currentUser);
            return ResponseEntity.ok(ApiResponse.success(data, data.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching dashboard data", e);
            DashboardResponse emptyResponse = DashboardResponse.builder()
                    .message("Dashboard temporarily unavailable")
                    .build();
            return ResponseEntity.ok(ApiResponse.success(emptyResponse, "Dashboard data unavailable"));
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> getTotalBalance() {
        try {
            User currentUser = getCurrentUser();
            java.math.BigDecimal balance = dashboardService.getTotalBalance(currentUser);
            return ResponseEntity.ok(ApiResponse.success(balance, "Total balance retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching total balance", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch total balance"));
        }
    }

    @GetMapping("/recent-transactions")
    public ResponseEntity<ApiResponse<List<RecentTransaction>>> getRecentTransactions(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            User currentUser = getCurrentUser();
            List<RecentTransaction> transactions = dashboardService.getRecentTransactions(currentUser);
            return ResponseEntity.ok(ApiResponse.success(transactions, "Recent transactions retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching recent transactions", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch recent transactions"));
        }
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<ApiResponse<List<DashboardResponse.MonthlyTrend>>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int months) {
        try {
            User currentUser = getCurrentUser();
            List<DashboardResponse.MonthlyTrend> trends = dashboardService.getMonthlyTrends(currentUser, months);
            return ResponseEntity.ok(ApiResponse.success(trends, "Monthly trends retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching monthly trends", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch monthly trends"));
        }
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<ApiResponse<java.util.Map<String, Double>>> getCategoryBreakdown() {
        try {
            User currentUser = getCurrentUser();
            java.util.Map<String, Double> breakdown = dashboardService.getCategoryBreakdown(currentUser);
            return ResponseEntity.ok(ApiResponse.success(breakdown, "Category breakdown retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching category breakdown", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch category breakdown"));
        }
    }

    @GetMapping("/budget-status")
    public ResponseEntity<ApiResponse<List<BudgetStatus>>> getBudgetStatus() {
        try {
            User currentUser = getCurrentUser();
            List<BudgetStatus> budgetStatus = dashboardService.getBudgetStatus(currentUser);
            return ResponseEntity.ok(ApiResponse.success(budgetStatus, "Budget status retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching budget status", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch budget status"));
        }
    }

    @GetMapping("/upcoming-bills")
    public ResponseEntity<ApiResponse<List<UpcomingBill>>> getUpcomingBills(
            @RequestParam(defaultValue = "30") int daysAhead) {
        try {
            User currentUser = getCurrentUser();
            List<UpcomingBill> upcomingBills = dashboardService.getUpcomingBills(currentUser, daysAhead);
            return ResponseEntity.ok(ApiResponse.success(upcomingBills, "Upcoming bills retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching upcoming bills", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch upcoming bills"));
        }
    }

    @GetMapping("/savings-goals")
    public ResponseEntity<ApiResponse<List<SavingsGoalDTO>>> getSavingsGoals() {
        try {
            User currentUser = getCurrentUser();
            List<SavingsGoalDTO> savingsGoals = dashboardService.getSavingsGoals(currentUser);
            return ResponseEntity.ok(ApiResponse.success(savingsGoals, "Savings goals retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching savings goals", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch savings goals"));
        }
    }

    @GetMapping("/financial-health")
    public ResponseEntity<ApiResponse<FinancialHealth>> getFinancialHealth() {
        try {
            User currentUser = getCurrentUser();
            FinancialHealth financialHealth = dashboardService.getFinancialHealth(currentUser);
            return ResponseEntity.ok(ApiResponse.success(financialHealth, "Financial health retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching financial health", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch financial health"));
        }
    }
}
