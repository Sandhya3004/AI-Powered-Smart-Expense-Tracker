package com.expense.controller;

import com.expense.dto.AuthRequest;
import com.expense.dto.AuthResponse;
import com.expense.dto.ApiResponse;
import com.expense.dto.UserDTO;
import com.expense.dto.SettingsDTO;
import com.expense.dto.ChangePasswordDTO;
import com.expense.dto.UpdateProfileDTO;
import com.expense.entity.User;
import com.expense.service.AuthService;
import com.expense.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody User user) {
        try {
            // Debug logging
            System.out.println("DEBUG - Incoming User: email=" + user.getEmail() + ", name=" + user.getName());
            System.out.println("DEBUG - Password value: " + (user.getPassword() != null ? "[PRESENT, length=" + user.getPassword().length() + "]" : "[NULL]"));
            
            // Validate password is present
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("Password is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            log.info("Registering user: {}", user.getEmail());
            authService.register(user);
            ApiResponse successResponse = new ApiResponse();
            successResponse.setSuccess(true);
            successResponse.setMessage("User registered successfully");
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            log.error("Registration error: {}", e.getMessage());
            if (e.getMessage().contains("already exists")) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("Email already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            // Authenticate user and get token
            String token = authService.authenticate(request);
            
            // Get user details
            User user = authService.getUserByEmail(request.getEmail());
            
            // Create session info
            String sessionId = java.util.UUID.randomUUID().toString();
            String expiresAt = LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ISO_DATE_TIME);
            String deviceInfo = request.getDeviceInfo();
            String ipAddress = getClientIpAddress(httpRequest);
            
            AuthResponse.SessionInfo sessionInfo = new AuthResponse.SessionInfo(
                sessionId, expiresAt, deviceInfo, ipAddress
            );
            
            // Return complete response
            UserDTO userDTO = UserDTO.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole())
                        .build();
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setType("Bearer");
            response.setUser(userDTO);
            response.setSession(sessionInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setToken(null);
            errorResponse.setType("Authentication failed: " + e.getMessage());
            errorResponse.setUser(null);
            errorResponse.setSession(null);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request) {
        // In a stateless JWT system, logout is handled client-side by removing the token
        // Server-side can optionally blacklist the token or track session invalidation
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Optional: Add token to blacklist or mark as invalidated
            // authService.blacklistToken(token);
            log.info("User logged out, token invalidated");
        }
        
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Logged out successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        try {
            authService.changePassword(dto.getOldPassword(), dto.getNewPassword());
            
            ApiResponse successResponse = new ApiResponse();
            successResponse.setSuccess(true);
            successResponse.setMessage("Password changed successfully");
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            log.error("Failed to change password: {}", e.getMessage());
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/upload-profile")
    public ResponseEntity<ApiResponse> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        log.info("Received upload-profile request, file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        try {
            if (file.isEmpty()) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("No file provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Validate file type
            String contentType = file.getContentType();
            log.info("File content type: {}", contentType);
            if (contentType == null || !contentType.startsWith("image/")) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("Only image files are allowed");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("File size must be less than 5MB");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("Calling fileStorageService.storeFile...");
            String imageUrl = fileStorageService.storeFile(file);
            log.info("File stored, updating profile image to: {}", imageUrl);
            authService.updateProfileImage(imageUrl);
            
            ApiResponse successResponse = new ApiResponse();
            successResponse.setSuccess(true);
            successResponse.setMessage("Profile image uploaded successfully");
            successResponse.setData(imageUrl);
            log.info("Upload successful, returning URL: {}", imageUrl);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            log.error("Failed to upload profile image: {}", e.getMessage(), e);
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to upload image: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        try {
            User updatedUser = authService.updateProfile(dto.getName().trim(), dto.getEmail().trim());
            
            ApiResponse successResponse = new ApiResponse();
            successResponse.setSuccess(true);
            successResponse.setMessage("Profile updated successfully");
            successResponse.setData(UserDTO.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .name(updatedUser.getName())
                .role(updatedUser.getRole())
                .build());
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            log.error("Failed to update profile: {}", e.getMessage());
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse> updateSettings(@Valid @RequestBody SettingsDTO dto) {
        try {
            authService.updateSettings(
                dto.getNotificationsEnabled(), 
                dto.getBudgetAlerts(), 
                dto.getDarkMode()
            );
            
            ApiResponse successResponse = new ApiResponse();
            successResponse.setSuccess(true);
            successResponse.setMessage("Settings updated successfully");
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            log.error("Failed to update settings: {}", e.getMessage());
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/validate-session")
    public ResponseEntity<AuthResponse.SessionInfo> validateSession(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // For now, just return a valid session response
            // In production, you'd validate against a session store
            String expiresAt = LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ISO_DATE_TIME);
            AuthResponse.SessionInfo sessionInfo = new AuthResponse.SessionInfo(
                sessionId, expiresAt, "validated", "127.0.0.1"
            );
            
            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}