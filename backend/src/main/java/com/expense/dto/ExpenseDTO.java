package com.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ExpenseDTO {
    private Long id;
    
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotBlank(message = "Type is required")
    private String type; // EXPENSE, INCOME, TRANSFER
    
    private String category;
    private String merchant;
    private String paymentType;
    private String account;
    private String currency;
    private String notes;
    private List<String> tags;
    
    private LocalDate expenseDate; // Optional - defaults to today if not provided
    
    private String source; // manual, voice, csv, ocr, etc.
    private Boolean isRecurring = false;
    private String recurrencePattern;
    private String status = "COMPLETED";
    
    // For transfers
    private String fromAccount;
    private String toAccount;
    
    // Response fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String userName;
}