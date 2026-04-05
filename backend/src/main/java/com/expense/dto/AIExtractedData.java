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
public class AIExtractedData {
    
    private String merchant;
    private BigDecimal amount;
    private LocalDate date;
    private String category;
    private Double confidence;
    private String description;
    private String rawText;
}
