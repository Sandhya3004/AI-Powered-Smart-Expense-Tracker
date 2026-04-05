package com.expense.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetDTO {
    private BigDecimal totalBudget;  // Changed from 'amount' to 'totalBudget' to match API
    private String month; // format yyyy-MM
    private String category;
    private String description;
}