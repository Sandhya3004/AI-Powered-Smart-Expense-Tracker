package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.Investment;
import com.expense.entity.User;
import com.expense.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
@Slf4j
public class InvestmentController extends BaseController {

    private final InvestmentService investmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Investment>>> getAllInvestments() {
        try {
            User user = getCurrentUser();
            List<Investment> investments = investmentService.getUserInvestments(user);
            return ResponseEntity.ok(ApiResponse.success(investments, "Investments retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching investments", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch investments: " + e.getMessage()));
        }
    }

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPortfolioSummary() {
        try {
            User user = getCurrentUser();
            Map<String, Object> portfolio = investmentService.getPortfolioSummary(user);
            return ResponseEntity.ok(ApiResponse.success(portfolio, "Portfolio summary retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching portfolio summary", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch portfolio summary: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Investment>> createInvestment(@RequestBody Investment investment) {
        try {
            User user = getCurrentUser();
            investment.setUser(user);
            Investment createdInvestment = investmentService.createInvestment(investment);
            return ResponseEntity.ok(ApiResponse.success(createdInvestment, "Investment created successfully"));
        } catch (Exception e) {
            log.error("Error creating investment", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create investment: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Investment>> updateInvestment(@PathVariable Long id, @RequestBody Investment investment) {
        try {
            User user = getCurrentUser();
            Investment existingInvestment = investmentService.getInvestmentById(id);
            
            if (existingInvestment == null || !existingInvestment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Investment not found or access denied"));
            }
            
            investment.setId(id);
            investment.setUser(user);
            Investment updatedInvestment = investmentService.updateInvestment(investment);
            return ResponseEntity.ok(ApiResponse.success(updatedInvestment, "Investment updated successfully"));
        } catch (Exception e) {
            log.error("Error updating investment", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update investment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvestment(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            Investment existingInvestment = investmentService.getInvestmentById(id);
            
            if (existingInvestment == null || !existingInvestment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Investment not found or access denied"));
            }
            
            investmentService.deleteInvestment(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Investment deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting investment", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete investment: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/update-value")
    public ResponseEntity<ApiResponse<Investment>> updateInvestmentValue(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User user = getCurrentUser();
            Investment existingInvestment = investmentService.getInvestmentById(id);
            
            if (existingInvestment == null || !existingInvestment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Investment not found or access denied"));
            }
            
            BigDecimal newCurrentValue = new BigDecimal(request.get("currentValue").toString());
            Investment updatedInvestment = investmentService.updateInvestmentValue(id, newCurrentValue);
            return ResponseEntity.ok(ApiResponse.success(updatedInvestment, "Investment value updated successfully"));
        } catch (Exception e) {
            log.error("Error updating investment value", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update investment value: " + e.getMessage()));
        }
    }
}
