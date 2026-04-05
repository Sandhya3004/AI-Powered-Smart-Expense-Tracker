package com.expense.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PredictionDTO {
    private BigDecimal predictedAmount;
    private String message;
}