package com.expense.controller;

import com.expense.entity.User;
import com.expense.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController extends BaseController {

    private final UserService userService;

    public TestController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Test endpoint to verify authentication is working
     */
    @GetMapping("/auth")
    public Map<String, Object> testAuth() {
        try {
            User user = getCurrentUser();
            
            return Map.of(
                "success", true,
                "message", "Authentication working correctly",
                "userId", user.getId().toString(),
                "userEmail", user.getEmail(),
                "authorities", SecurityContextHolder.getContext().getAuthentication().getAuthorities()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis()
        );
    }
}
