package com.expense.util;

import com.expense.entity.Expense;
import com.expense.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public class CSVHelper {

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b"
    );

    public static class ImportResult {
        private final int totalRows;
        private final int successful;
        private final int failed;
        private final List<String> errors;
        private final List<Expense> expenses;

        public ImportResult(int totalRows, int successful, int failed, List<String> errors, List<Expense> expenses) {
            this.totalRows = totalRows;
            this.successful = successful;
            this.failed = failed;
            this.errors = errors;
            this.expenses = expenses;
        }

        public int getTotalRows() { return totalRows; }
        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }
        public List<String> getErrors() { return errors; }
        public List<Expense> getExpenses() { return expenses; }
    }

    public static ImportResult parseCSV(MultipartFile file, User user) {
        List<Expense> expenses = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successful = 0;
        int failed = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                totalRows++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Skip header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                try {
                    Expense expense = parseExpenseFromCSVLine(line, user, lineNumber);
                    if (expense != null) {
                        expenses.add(expense);
                        successful++;
                    } else {
                        failed++;
                        errors.add("Line " + lineNumber + ": Invalid data format");
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    log.error("Error parsing CSV line {}: {}", lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            errors.add("Failed to read file: " + e.getMessage());
        }

        return new ImportResult(totalRows, successful, failed, errors, expenses);
    }

    private static Expense parseExpenseFromCSVLine(String line, User user, int lineNumber) {
        String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        
        if (values.length < 3) {
            throw new IllegalArgumentException("At least 3 columns required (Date, Description, Amount)");
        }

        // Clean and trim values
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].replaceAll("^\"|\"$", "").trim();
        }

        // Parse date (try different formats)
        String dateStr = values[0];
        LocalDate date = parseDate(dateStr);
        if (date == null) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }

        // Parse description
        String description = values.length > 1 ? values[1] : "";
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }

        // Parse amount
        String amountStr = values.length > 2 ? values[2] : "0";
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.replaceAll("[^\\d.-]", ""));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amountStr);
        }

        // Parse category (optional)
        String category = values.length > 3 ? values[3] : "Other";
        if (category.isEmpty()) {
            category = "Other";
        }

        // Parse notes (optional)
        String notes = values.length > 4 ? values[4] : "";

        return Expense.builder()
                .user(user)
                .description(description)
                .amount(amount)
                .category(category)
                .expenseDate(date)
                .notes(notes)
                .source("CSV")
                .type("EXPENSE")
                .currency("INR")
                .paymentType("Cash")
                .account("Cash")
                .merchant("")
                .build();
    }

    private static LocalDate parseDate(String dateStr) {
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M-d-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        return null;
    }

    public static boolean isValidCSVFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        return filename != null && (filename.toLowerCase().endsWith(".csv"));
    }
}
