package com.expense.dto;

import jakarta.validation.constraints.*;
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
public class ReceiptDTO {
    
    @Digits(integer = 10, fraction = 2, message = "Amount must have valid format")
    private BigDecimal amount;
    
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @Size(max = 100, message = "Merchant must not exceed 100 characters")
    private String merchant;
    
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @Pattern(regexp = "^(manual|ocr|csv|sms)$", message = "Source must be manual, ocr, csv, or sms")
    private String source = "manual";
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    private String imagePath;
    
    private Double confidence;
    
    private String type = "EXPENSE";
    
    private String status = "COMPLETED";
}
