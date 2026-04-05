package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponseDTO {
    private Long id;
    private BigDecimal amount;
    private String type;
    private String description;
    private String category;
    private String merchant;
    private String paymentType;
    private String account;
    private String currency;
    private LocalDate expenseDate;
    private String source;
    private String notes;
    private List<String> tags;
    private Boolean isRecurring;
    private String recurrencePattern;
    private String status;
    private String fromAccount;
    private String toAccount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}