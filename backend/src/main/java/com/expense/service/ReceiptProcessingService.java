package com.expense.service;

import com.expense.dto.ReceiptDTO;
import com.expense.entity.Receipt;
import com.expense.entity.User;
import com.expense.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptProcessingService {

    private final ReceiptRepository receiptRepository;
    private final AuthService authService;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public Receipt processReceipt(MultipartFile file) throws IOException {
        User user = authService.getCurrentUser();
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Save file
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        try {
            // Process image with Tesseract OCR
            String extractedText = performOCR(filePath.toFile());
            log.info("OCR extracted text: {}", extractedText);
            
            // Call AI microservice for structured data extraction
            Map<String, Object> aiData = callAIService(extractedText);
            log.info("AI extracted data: {}", aiData);
            
            // Create receipt record with AI-extracted data
            Receipt receipt = Receipt.builder()
                    .user(user)
                    .imagePath(fileName)
                    .extractedText(extractedText)
                    .merchant((String) aiData.getOrDefault("merchant", "Unknown Merchant"))
                    .amount(new java.math.BigDecimal(aiData.getOrDefault("amount", "0").toString()))
                    .date(java.time.LocalDate.parse((String) aiData.getOrDefault("date", java.time.LocalDate.now().toString())))
                    .category((String) aiData.getOrDefault("category", "Other"))
                    .confidence(((Number) aiData.getOrDefault("confidence", 0.0)).doubleValue())
                    .source("ocr")
                    .status("COMPLETED")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            receipt = receiptRepository.save(receipt);
            log.info("Receipt saved with ID: {}", receipt.getId());
            
            return receipt;
            
        } catch (Exception e) {
            log.error("Failed to process receipt", e);
            // Create receipt with error status
            Receipt errorReceipt = Receipt.builder()
                    .user(user)
                    .imagePath(fileName)
                    .extractedText("OCR processing failed")
                    .merchant("Error")
                    .amount(java.math.BigDecimal.ZERO)
                    .date(java.time.LocalDate.now())
                    .category("Error")
                    .confidence(0.0)
                    .source("ocr")
                    .status("FAILED")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            return receiptRepository.save(errorReceipt);
        }
    }
    
    private String performOCR(File imageFile) {
        try {
            log.info("Starting OCR processing for file: {}", imageFile.getName());
            
            // Initialize Tesseract
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata");
            tesseract.setLanguage("eng");
            tesseract.setOcrEngineMode(1); // OEM_DEFAULT
            tesseract.setPageSegMode(6); // PSM_AUTO
            
            // Read image
            BufferedImage image = ImageIO.read(imageFile);
            
            // Perform OCR
            String result = tesseract.doOCR(image);
            
            log.info("OCR processing completed for file: {}", imageFile.getName());
            return result;
            
        } catch (Exception e) {
            log.error("OCR processing failed for file: {}", imageFile.getName(), e);
            throw new RuntimeException("Failed to process receipt image with OCR", e);
        }
    }
    
    private Map<String, Object> callAIService(String ocrText) {
        try {
            WebClient webClient = webClientBuilder.build();
            
            // Prepare request payload
            Map<String, String> request = new HashMap<>();
            request.put("text", ocrText);
            
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(request);
            
            // Call AI microservice
            Map<String, Object> response = webClient.post()
                    .uri(aiServiceUrl + "/ocr")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            log.info("AI service response: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to call AI service", e);
            // Fallback to basic extraction
            return fallbackExtraction(ocrText);
        }
    }
    
    private Map<String, Object> fallbackExtraction(String ocrText) {
        Map<String, Object> result = new HashMap<>();
        
        // Simple regex-based extraction as fallback
        java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("Rs\\s*([0-9,]+\\.?[0-9]*)");
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{2}[\\/-]\\d{2}[\\/-]\\d{2,4})");
        
        // Extract amount
        java.util.regex.Matcher amountMatcher = amountPattern.matcher(ocrText);
        if (amountMatcher.find()) {
            String amountStr = amountMatcher.group(1).replace(",", "");
            try {
                double amount = Double.parseDouble(amountStr);
                result.put("amount", amount);
            } catch (NumberFormatException e) {
                result.put("amount", 0.0);
            }
        } else {
            result.put("amount", 0.0);
        }
        
        // Extract date
        java.util.regex.Matcher dateMatcher = datePattern.matcher(ocrText);
        if (dateMatcher.find()) {
            result.put("date", java.time.LocalDate.now().toString());
        } else {
            result.put("date", java.time.LocalDate.now().toString());
        }
        
        // Set defaults
        result.put("merchant", "Unknown Merchant");
        result.put("category", "Other");
        result.put("confidence", 0.3); // Low confidence for fallback
        
        return result;
    }
    
    public Receipt getReceipt(Long receiptId) {
        User user = authService.getCurrentUser();
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));
    }
    
    public List<Receipt> getUserReceipts() {
        User user = authService.getCurrentUser();
        return receiptRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public void deleteReceipt(Long receiptId) {
        User user = authService.getCurrentUser();
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));
        
        // Verify ownership
        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete file
        try {
            Path filePath = Paths.get(uploadDir, receipt.getImagePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete receipt file: {}", receipt.getImagePath());
        }
        
        // Delete record
        receiptRepository.delete(receipt);
    }
}
