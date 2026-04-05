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
public class BillReminderDTO {
    
    private Long id;
    private String billName;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String category;
    private String paymentMethod;
    private String recurrencePattern;
    private boolean isPaid;
    private String status; // PENDING, PAID, OVERDUE
    private List<Integer> reminderDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
}
