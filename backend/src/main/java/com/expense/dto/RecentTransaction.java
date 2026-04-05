package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransaction {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String category;
    private String type; // INCOME or EXPENSE
    private LocalDateTime date;
    private String currency;
    private String source;
    private Long expenseId;
    private Long userId;
}
