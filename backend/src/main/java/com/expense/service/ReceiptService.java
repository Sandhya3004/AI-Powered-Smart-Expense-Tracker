package com.expense.service;

import com.expense.dto.ReceiptDTO;
import com.expense.entity.Expense;
import com.expense.entity.Receipt;
import com.expense.entity.User;
import com.expense.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final AuthService authService;

    public Map<String, Object> uploadReceipt(MultipartFile file, Long expenseId) {
        try {
            User currentUser = authService.getCurrentUser();
            
            // Mock file processing - in production, store file in cloud storage
            String fileName = "receipt_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String imageUrl = "/uploads/" + fileName;
            
            Receipt receipt = Receipt.builder()
                    .user(currentUser)
                    .imagePath(fileName)
                    .extractedText("")
                    .merchant("")
                    .amount(BigDecimal.ZERO)
                    .date(LocalDate.now())
                    .category("")
                    .confidence(0.0)
                    .notes("")
                    .tags("")
                    .source("manual")
                    .type("EXPENSE")
                    .status("PENDING")
                    .build();
            
            // Link to expense if provided
            if (expenseId != null) {
                // Mock expense linking
                log.info("Linking receipt to expense: {}", expenseId);
            }
            
            Receipt savedReceipt = receiptRepository.save(receipt);
            
            // Auto-process receipt
            processReceipt(savedReceipt.getId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("receipt", savedReceipt);
            result.put("message", "Receipt uploaded successfully");
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to upload receipt", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to upload receipt: " + e.getMessage());
            return error;
        }
    }

    public List<Receipt> getAllReceipts() {
        User currentUser = authService.getCurrentUser();
        return receiptRepository.findByUserOrderByCreatedAtDesc(currentUser);
    }

    public Receipt getReceiptById(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));
    }

    public byte[] downloadReceipt(Long id) {
        Receipt receipt = getReceiptById(id);
        
        // Mock file download - in production, retrieve from storage
        try {
            // Return mock image data
            return "mock-image-data".getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download receipt", e);
        }
    }

    public void deleteReceipt(Long id) {
        Receipt receipt = getReceiptById(id);
        receiptRepository.delete(receipt);
    }

    public Map<String, Object> processReceipt(Long id) {
        Receipt receipt = getReceiptById(id);
        
        try {
            receipt.setStatus("processing");
            receiptRepository.save(receipt);
            
            // Mock OCR processing
            Thread.sleep(2000); // Simulate processing time
            
            // Mock extracted data
            String mockText = "STARBUCKS\nCoffee\n$12.50\n03/15/2024\n123 Main St";
            
            receipt.setExtractedText(mockText);
            receipt.setAmount(extractAmountFromText(mockText));
            receipt.setMerchant(extractMerchantFromText(mockText));
            receipt.setDate(extractDateFromText(mockText));
            receipt.setCategory("Food & Beverages");
            receipt.setConfidence(0.85);
            
            Receipt updatedReceipt = receiptRepository.save(receipt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("extractedData", Map.of(
                "amount", updatedReceipt.getAmount(),
                "merchant", updatedReceipt.getMerchant(),
                "date", updatedReceipt.getDate(),
                "text", updatedReceipt.getExtractedText()
            ));
            
            return result;
            
        } catch (Exception e) {
            receipt.setStatus("failed");
            receiptRepository.save(receipt);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to process receipt: " + e.getMessage());
            return error;
        }
    }

    public Map<String, Object> extractTextFromImage(MultipartFile file) {
        try {
            // Mock OCR extraction
            String mockText = "WALMART\nGroceries\n$87.43\n03/15/2024\nReceipt #12345";
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("text", mockText);
            result.put("extractedData", Map.of(
                "amount", extractAmountFromText(mockText),
                "merchant", extractMerchantFromText(mockText),
                "date", extractDateFromText(mockText)
            ));
            
            return result;
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to extract text: " + e.getMessage());
            return error;
        }
    }

    public List<Receipt> getReceiptsByExpense(Long expenseId) {
        // Mock implementation
        return new ArrayList<>();
    }

    public void linkReceiptToExpense(Long receiptId, Long expenseId) {
        Receipt receipt = getReceiptById(receiptId);
        // Mock expense linking
        log.info("Linking receipt {} to expense {}", receiptId, expenseId);
        receiptRepository.save(receipt);
    }

    public List<Receipt> searchReceipts(String query) {
        User currentUser = authService.getCurrentUser();
        // Mock search implementation
        return receiptRepository.findByUserOrderByCreatedAtDesc(currentUser);
    }

    private BigDecimal extractAmountFromText(String text) {
        Pattern pattern = Pattern.compile("\\$(\\d+(?:\\.\\d{2})?)");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        
        return BigDecimal.ZERO;
    }

    private String extractMerchantFromText(String text) {
        String[] lines = text.split("\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            if (!firstLine.matches(".*\\d+.*") && firstLine.length() > 2) {
                return firstLine;
            }
        }
        
        return "Unknown Merchant";
    }
    
    private LocalDate extractDateFromText(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.matches("\\d{2}/\\d{2}/\\d{4}")) {
                String[] parts = line.split("/");
                try {
                    int month = Integer.parseInt(parts[0]);
                    int day = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    return LocalDate.of(year, month, day);
                } catch (Exception e) {
                    // Return current date if parsing fails
                    return LocalDate.now();
                }
            }
        }
        
        return LocalDate.now();
    }
}
