package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealth {
    private int score; // 0-100
    private String rating; // Poor, Fair, Good, Excellent
    private BigDecimal savingsRate;
    private BigDecimal debtRatio;
    private List<String> recommendations;
    private String lastCalculated;
}
