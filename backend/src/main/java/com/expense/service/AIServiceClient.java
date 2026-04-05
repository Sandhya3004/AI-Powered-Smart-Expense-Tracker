package com.expense.service;

import com.expense.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:https://ai-service-b4ex.onrender.com}")
    private String aiServiceUrl;

    public Map<String, Object> processVoice(String audioData, String format) {
        try {
            log.info("Sending voice data to AI service for processing");
            
            Map<String, Object> request = Map.of(
                "audioData", audioData,
                "format", format
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiServiceUrl + "/nlp/process-voice",
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Voice processing successful");
                return response.getBody();
            } else {
                log.error("Voice processing failed with status: {}", response.getStatusCode());
                return Map.of("success", false, "error", "AI service unavailable");
            }
        } catch (Exception e) {
            log.error("Error processing voice with AI service", e);
            return Map.of("success", false, "error", "Failed to process voice: " + e.getMessage());
        }
    }

    public Map<String, Object> processVoiceFile(MultipartFile audioFile) {
        try {
            log.info("Sending voice file to AI service for processing: {}", audioFile.getOriginalFilename());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio", new ByteArrayResource(audioFile.getBytes(), audioFile.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiServiceUrl + "/nlp/process-voice",
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Voice file processing successful");
                return response.getBody();
            } else {
                log.error("Voice file processing failed with status: {}", response.getStatusCode());
                return Map.of("success", false, "error", "AI service unavailable");
            }
        } catch (IOException e) {
            log.error("Error reading voice file", e);
            return Map.of("success", false, "error", "Failed to read file");
        } catch (Exception e) {
            log.error("Error processing voice file with AI service", e);
            return Map.of("success", false, "error", "Failed to process voice file: " + e.getMessage());
        }
    }

    public Map<String, Object> processReceipt(MultipartFile file) {
        try {
            log.info("Sending receipt to AI service for OCR processing");
            
            // Compress image before sending
            byte[] compressedImageBytes = compressImage(file.getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("receipt", new ByteArrayResource(compressedImageBytes, file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiServiceUrl + "/ocr/receipt",
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Receipt OCR processing successful");
                return response.getBody();
            } else {
                log.error("Receipt OCR processing failed with status: {}", response.getStatusCode());
                return Map.of("success", false, "error", "AI service unavailable");
            }
        } catch (IOException e) {
            log.error("Error reading receipt file", e);
            return Map.of("success", false, "error", "Failed to read file");
        } catch (Exception e) {
            log.error("Error processing receipt with AI service", e);
            return Map.of("success", false, "error", "Failed to process receipt: " + e.getMessage());
        }
    }

    private byte[] compressImage(byte[] imageBytes) throws IOException {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                // If image can't be read, return original bytes
                return imageBytes;
            }

            // Resize to max width 1024, maintain aspect ratio
            int maxWidth = 1024;
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            if (originalWidth > maxWidth) {
                int newHeight = (int) ((double) maxWidth / originalWidth * originalHeight);
                
                // Create scaled image
                Image scaledImage = originalImage.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                
                // Create new buffered image
                BufferedImage compressed = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = compressed.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();
                
                // Convert to bytes with JPEG compression
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(compressed, "jpg", baos);
                byte[] result = baos.toByteArray();
                
                log.info("Image compressed from {}x{} to {}x{}, size reduced from {} to {} bytes", 
                        originalWidth, originalHeight, maxWidth, newHeight, 
                        imageBytes.length, result.length);
                
                return result;
            }
            
            return imageBytes;
        } catch (Exception e) {
            log.error("Error compressing image", e);
            // Return original bytes if compression fails
            return imageBytes;
        }
    }

    public boolean checkHealth() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                aiServiceUrl + "/health",
                String.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("AI service health check failed", e);
            return false;
        }
    }
}
