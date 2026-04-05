package com.expense.controller;

import com.expense.entity.User;
import com.expense.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class BaseController {

    @Autowired
    private UserRepository userRepository;

    protected User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("No authentication found in security context");
                throw new RuntimeException("User not authenticated");
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                // Reload from DB to ensure we have a managed entity
                return userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found in database"));
            } else if (principal instanceof String) {
                String email = (String) principal;
                return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            } else {
                log.warn("Unknown principal type: {}", principal.getClass().getName());
                throw new RuntimeException("Invalid authentication principal");
            }
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            throw new RuntimeException("Authentication error: " + e.getMessage());
        }
    }

    protected Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    protected String getCurrentUserEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
}
