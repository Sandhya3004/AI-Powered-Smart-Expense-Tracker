package com.expense.service;

import com.expense.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // In-memory storage for notifications (in production, use database or message queue)
    private final Map<String, Object> notificationStorage = new ConcurrentHashMap<>();

    /**
     * Send notification to user
     */
    public void sendNotification(User user, String title, String message) {
        try {
            // Create notification object
            Map<String, Object> notification = Map.of(
                "userId", user.getId(),
                "userEmail", user.getEmail(),
                "title", title,
                "message", message,
                "timestamp", LocalDateTime.now(),
                "type", "INFO",
                "read", false
            );

            // Store notification (in production, this would be sent via email, SMS, push notification)
            String notificationId = user.getId() + "_" + System.currentTimeMillis();
            notificationStorage.put(notificationId, notification);

            // Log the notification
            log.info("Notification sent to user {}: {} - {}", user.getEmail(), title, message);

            // In a real implementation, you would:
            // 1. Send email notification
            // 2. Send SMS if configured
            // 3. Send push notification to mobile app
            // 4. Store in database for in-app notifications

        } catch (Exception e) {
            log.error("Error sending notification to user: {}", user.getEmail(), e);
        }
    }

    /**
     * Send bill reminder notification
     */
    public void sendBillReminder(User user, String billName, String dueDate, String amount) {
        String title = "Bill Reminder";
        String message = String.format("Your bill '%s' is due on %s. Amount: Rs %s", 
            billName, dueDate, amount);
        
        sendNotification(user, title, message);
    }

    /**
     * Send overdue bill notification
     */
    public void sendOverdueBillNotification(User user, String billName, String overdueDays, String amount) {
        String title = "Overdue Bill Alert";
        String message = String.format("Your bill '%s' is overdue by %s days. Amount: Rs %s. Please pay immediately.", 
            billName, overdueDays, amount);
        
        sendNotification(user, title, message);
    }

    /**
     * Send group expense notification
     */
    public void sendGroupExpenseNotification(User user, String groupName, String action, String amount) {
        String title = "Group Expense Update";
        String message = String.format("You have been %s a group expense '%s' for Rs %s", 
            action, groupName, amount);
        
        sendNotification(user, title, message);
    }

    /**
     * Send budget alert notification
     */
    public void sendBudgetAlert(User user, String category, String spentAmount, String budgetAmount, String percentage) {
        String title = "Budget Alert";
        String message = String.format("You have spent %s (%s) of your %s budget for %s", 
            spentAmount, percentage, budgetAmount, category);
        
        sendNotification(user, title, message);
    }

    /**
     * Send transaction confirmation
     */
    public void sendTransactionConfirmation(User user, String transactionType, String amount, String description) {
        String title = "Transaction Confirmation";
        String message = String.format("Your %s transaction of Rs %s for '%s' has been successfully recorded.", 
            transactionType, amount, description);
        
        sendNotification(user, title, message);
    }

    /**
     * Send welcome notification
     */
    public void sendWelcomeNotification(User user) {
        String title = "Welcome to Smart Expense Tracker";
        String message = "Welcome! Your account has been successfully created. Start tracking your expenses now!";
        
        sendNotification(user, title, message);
    }

    /**
     * Send security alert
     */
    public void sendSecurityAlert(User user, String action, String timestamp, String device) {
        String title = "Security Alert";
        String message = String.format("A security action was performed on your account: %s at %s from %s", 
            action, timestamp, device);
        
        sendNotification(user, title, message);
    }

    /**
     * Get user notifications
     */
    public Map<String, Object> getUserNotifications(User user, int limit) {
        // In production, this would query the database
        return Map.of(
            "notifications", notificationStorage.values().stream()
                    .filter(notification -> {
                        Object userIdObj = notification instanceof Map ? ((Map<?, ?>) notification).get("userId") : null;
                        return user.getId().equals(userIdObj);
                    })
                    .limit(limit)
                    .toList(),
            "unreadCount", notificationStorage.values().stream()
                    .filter(notification -> {
                        Object userIdObj = notification instanceof Map ? ((Map<?, ?>) notification).get("userId") : null;
                        Object readObj = notification instanceof Map ? ((Map<?, ?>) notification).get("read") : null;
                        return user.getId().equals(userIdObj) && !Boolean.TRUE.equals(readObj);
                    })
                    .count()
        );
    }

    /**
     * Mark notification as read
     */
    public void markNotificationAsRead(String notificationId) {
        Object notification = notificationStorage.get(notificationId);
        if (notification instanceof Map) {
            Map<String, Object> notifMap = (Map<String, Object>) notification;
            notifMap.put("read", true);
            notificationStorage.put(notificationId, notifMap);
        }
    }

    /**
     * Delete notification
     */
    public void deleteNotification(String notificationId) {
        notificationStorage.remove(notificationId);
    }

    /**
     * Clear all notifications for user
     */
    public void clearUserNotifications(User user) {
        notificationStorage.entrySet().removeIf(entry -> {
            Object notification = entry.getValue();
            return notification instanceof Map && 
                   ((Map<String, Object>) notification).get("userId").equals(user.getId());
        });
    }
}
