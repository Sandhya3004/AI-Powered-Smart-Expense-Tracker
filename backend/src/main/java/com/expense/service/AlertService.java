package com.expense.service;

import com.expense.entity.Alert;
import com.expense.entity.User;
import com.expense.exception.ResourceNotFoundException;
import com.expense.exception.SecurityException;
import com.expense.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final AuthService authService;

    public List<Alert> getUserAlerts() {
        User user = authService.getCurrentUser();
        log.debug("Fetching alerts for user: {}", user.getId());
        return alertRepository.findByUserAndReadFalse(user);
    }

    public void markAsRead(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        if (!alert.getUser().getId().equals(authService.getCurrentUser().getId())) {
            throw new SecurityException("Access denied");
        }
        alert.setRead(true);
        alertRepository.save(alert);
        log.info("Marked alert as read: {}", id);
    }
}