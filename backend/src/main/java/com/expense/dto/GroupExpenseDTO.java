package com.expense.dto;

import jakarta.validation.constraints.*;
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
public class GroupExpenseDTO {
    
    private Long id;
    private Long groupId;
    
    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String groupName;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotNull(message = "Total amount is required")
    @Digits(integer = 10, fraction = 2, message = "Total amount must have valid format")
    private BigDecimal totalAmount;
    
    private String category;
    private String paidBy;
    private String currency = "INR";
    private String status;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @NotEmpty(message = "At least one participant is required")
    private List<String> participantEmails;
    
    private String paymentMethod;
    private String splitType = "EQUAL"; // EQUAL, PERCENTAGE, CUSTOM
    
    // For CSV import/export
    public static GroupExpenseDTO fromCSV(String[] csvLine) {
        try {
            return GroupExpenseDTO.builder()
                    .expenseDate(LocalDate.parse(csvLine[0]))
                    .description(csvLine[1])
                    .totalAmount(new BigDecimal(csvLine[2]))
                    .category(csvLine[3])
                    .paidBy(csvLine[4])
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Invalid CSV format: " + String.join(",", csvLine), e);
        }
    }
    
    public String[] toCSV() {
        return new String[] {
            expenseDate != null ? expenseDate.toString() : "",
            description != null ? description : "",
            totalAmount != null ? totalAmount.toString() : "0.00",
            category != null ? category : "",
            paidBy != null ? paidBy : ""
        };
    }
}
