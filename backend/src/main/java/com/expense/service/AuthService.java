package com.expense.service;

import com.expense.config.JwtUtil;
import com.expense.dto.AuthRequest;
import com.expense.entity.User;
import com.expense.exception.UnauthorizedException;
import com.expense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public void register(User user) {
        log.info("Registering new user: {}", user.getEmail());
        
        // Validate password is present
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        
        // Check if user already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
    }

    public String authenticate(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        return jwtUtil.generateToken(userDetails);
    }

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found with email: " + email));
    }

    public void changePassword(String oldPassword, String newPassword) {
        User currentUser = getCurrentUser();
        
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }
        
        // Update password
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
        log.info("Password changed for user: {}", currentUser.getEmail());
    }

    public void updateProfileImage(String imageUrl) {
        User currentUser = getCurrentUser();
        currentUser.setProfileImage(imageUrl);
        userRepository.save(currentUser);
        log.info("Profile image updated for user: {}", currentUser.getEmail());
    }

    public User updateProfile(String name, String email) {
        User currentUser = getCurrentUser();
        
        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        
        // Check if email is already taken by another user
        if (!email.equals(currentUser.getEmail())) {
            userRepository.findByEmail(email).ifPresent(u -> {
                throw new RuntimeException("Email is already in use");
            });
        }
        
        currentUser.setName(name);
        currentUser.setEmail(email);
        User updatedUser = userRepository.save(currentUser);
        log.info("Profile updated for user: {}", updatedUser.getEmail());
        return updatedUser;
    }

    public void updateSettings(Boolean notificationsEnabled, Boolean budgetAlerts, Boolean darkMode) {
        User currentUser = getCurrentUser();
        
        if (notificationsEnabled != null) {
            currentUser.setNotificationsEnabled(notificationsEnabled);
        }
        if (budgetAlerts != null) {
            currentUser.setBudgetAlerts(budgetAlerts);
        }
        if (darkMode != null) {
            currentUser.setDarkMode(darkMode);
        }
        
        userRepository.save(currentUser);
        log.info("Settings updated for user: {}", currentUser.getEmail());
    }
}