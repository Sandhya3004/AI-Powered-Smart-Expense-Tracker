package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.User;
import com.expense.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController extends BaseController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUserProfile() {
        try {
            User user = super.getCurrentUser();
            // Remove sensitive information from response
            user.setPassword(null);
            return ResponseEntity.ok(ApiResponse.success(user, "User profile retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching user profile", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch user profile: " + e.getMessage()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestBody Map<String, Object> updates) {
        try {
            User currentUser = getCurrentUser();
            
            // Update allowed fields
            if (updates.containsKey("name")) {
                currentUser.setName((String) updates.get("name"));
            }
            if (updates.containsKey("email")) {
                currentUser.setEmail((String) updates.get("email"));
            }
            if (updates.containsKey("phone")) {
                currentUser.setPhone((String) updates.get("phone"));
            }
            if (updates.containsKey("monthlyBudget")) {
                currentUser.setMonthlyBudget(new java.math.BigDecimal(updates.get("monthlyBudget").toString()));
            }
            if (updates.containsKey("currency")) {
                currentUser.setCurrency((String) updates.get("currency"));
            }
            if (updates.containsKey("theme")) {
                currentUser.setTheme((String) updates.get("theme"));
            }
            
            User updatedUser = userService.updateUser(currentUser);
            
            // Remove password from response
            updatedUser.setPassword(null);
            return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully"));
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update profile: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody Map<String, String> passwordRequest) {
        try {
            User currentUser = getCurrentUser();
            String oldPassword = passwordRequest.get("oldPassword");
            String newPassword = passwordRequest.get("newPassword");
            
            // Validate old password
            if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Current password is incorrect"));
            }
            
            // Validate new password
            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("New password must be at least 8 characters long"));
            }
            
            // Update password
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "Password updated successfully"));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to change password: " + e.getMessage()));
        }
    }

    @PostMapping("/export-data")
    public ResponseEntity<ApiResponse<String>> exportUserData() {
        try {
            User currentUser = getCurrentUser();
            String csvData = userService.exportUserData(currentUser);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=expense_data.csv")
                    .header("Content-Type", "text/csv")
                    .body(ApiResponse.success(csvData, "Data exported successfully"));
        } catch (Exception e) {
            log.error("Error exporting user data", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to export data: " + e.getMessage()));
        }
    }
}
