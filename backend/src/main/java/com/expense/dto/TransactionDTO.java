package com.expense.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(EXPENSE|INCOME|TRANSFER)$", message = "Type must be EXPENSE, INCOME, or TRANSFER")
    private String type;
    
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    @Size(max = 100, message = "Merchant must not exceed 100 characters")
    private String merchant;
    
    @Size(max = 50, message = "Payment type must not exceed 50 characters")
    private String paymentType;
    
    @Size(max = 100, message = "Account must not exceed 100 characters")
    private String account;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @Pattern(regexp = "^(manual|csv|sms|ocr)$", message = "Source must be manual, csv, sms, or ocr")
    private String source = "manual";
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    private List<String> tags;
    private Boolean isRecurring = false;
    private String recurrencePattern;
    private String status = "COMPLETED";
    
    // Transfer specific fields
    @Size(max = 100, message = "From account must not exceed 100 characters")
    private String fromAccount;
    
    @Size(max = 100, message = "To account must not exceed 100 characters")
    private String toAccount;
    
    // For bulk operations
    private List<TransactionDTO> transactions;
    
    // Filters for search
    private String search;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> categories;
    private List<String> accounts;
    private List<String> types;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String sortBy = "date";
    private String sortDirection = "desc";
    private Integer page = 0;
    private Integer size = 20;
}
