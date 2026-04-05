package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {
    
    private String description;
    private BigDecimal amount;
    private String category;
    private LocalDate date;
    private String type;
    private String paymentType;
    private String notes;
}
