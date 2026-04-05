package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatus {
    private String category;
    private BigDecimal budgetAmount;
    private BigDecimal spent;
    private BigDecimal remaining;
    private double percentageUsed;
    private String status; // ON_TRACK, OVER_BUDGET, WARNING
}
