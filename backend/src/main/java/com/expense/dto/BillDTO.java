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
public class BillDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String category;
    private boolean paid;
    private String status; // PENDING, PAID, OVERDUE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
