package com.expense.service;

import com.expense.dto.ExpenseRequest;
import com.expense.entity.Expense;
import com.expense.entity.User;
import com.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CSVService {

    private final ExpenseRepository expenseRepository;

    public List<Expense> parseCSV(MultipartFile file, User user) {
        try {
            List<Expense> expenses = new ArrayList<>();
            
            try (Reader reader = new InputStreamReader(file.getInputStream());
                 BufferedReader br = new BufferedReader(reader)) {
                
                Iterable<CSVRecord> records = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .parse(br);
                
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                
                for (CSVRecord record : records) {
                    try {
                        String description = StringUtils.trimToEmpty(record.get("Description"));
                        String amountStr = StringUtils.trimToEmpty(record.get("Amount"));
                        String category = StringUtils.trimToEmpty(record.get("Category"));
                        String dateStr = StringUtils.trimToEmpty(record.get("Date"));
                        String type = StringUtils.trimToEmpty(record.get("Type"));
                        String paymentType = StringUtils.trimToEmpty(record.get("Payment Type"));
                        String notes = StringUtils.trimToEmpty(record.get("Notes"));
                        
                        if (StringUtils.isNotBlank(description) && StringUtils.isNotBlank(amountStr)) {
                            Expense expense = Expense.builder()
                                    .description(description)
                                    .amount(new BigDecimal(amountStr.replaceAll("[^0-9.]", "")))
                                    .category(category)
                                    .expenseDate(LocalDate.parse(dateStr, dateFormatter))
                                    .type(type != null ? type : "EXPENSE")
                                    .paymentType(paymentType)
                                    .notes(notes)
                                    .source("csv")
                                    .user(user)
                                    .build();
                            
                            expenses.add(expense);
                        }
                    } catch (Exception e) {
                        log.warn("Skipping invalid CSV record: {}", record, e);
                    }
                }
            }
            
            return expenseRepository.saveAll(expenses);
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }
}
