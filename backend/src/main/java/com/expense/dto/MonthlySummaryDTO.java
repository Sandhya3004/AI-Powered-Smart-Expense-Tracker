package com.expense.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
public class MonthlySummaryDTO {
    private YearMonth month;
    private BigDecimal totalExpenses;
    private long expenseCount;
    private BigDecimal budget;
    private BigDecimal remaining;
}