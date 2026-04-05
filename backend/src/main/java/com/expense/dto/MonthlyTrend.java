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
public class MonthlyTrend {
    private String month; // "2026-04"
    private BigDecimal expenses;
    private BigDecimal income;
    private BigDecimal savings;
}
