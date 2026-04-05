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
public class UpcomingBill {
    private Long id;
    private String title;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String category;
    private boolean isPaid;
    private String status; // PENDING, PAID, OVERDUE
    private Integer daysUntilDue;
    private Long billReminderId;
}
