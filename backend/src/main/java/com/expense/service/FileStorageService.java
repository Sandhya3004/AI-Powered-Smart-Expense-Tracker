package com.expense.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:}")
    private String baseUrl;

    public String storeFile(MultipartFile file) {
        try {
            log.info("Storing file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "profiles");
            if (!Files.exists(uploadPath)) {
                log.info("Creating upload directory: {}", uploadPath.toAbsolutePath());
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path targetLocation = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", targetLocation.toAbsolutePath());

            // Return URL or path
            String resultUrl;
            if (baseUrl != null && !baseUrl.isEmpty()) {
                resultUrl = baseUrl + "/uploads/profiles/" + filename;
            } else {
                resultUrl = "/uploads/profiles/" + filename;
            }
            log.info("Returning image URL: {}", resultUrl);
            return resultUrl;

        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath.replace("/uploads/", ""));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }
}
