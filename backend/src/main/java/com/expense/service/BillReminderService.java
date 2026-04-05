package com.expense.service;

import com.expense.dto.BillReminderDTO;
import com.expense.entity.BillReminder;
import com.expense.entity.User;
import com.expense.repository.BillReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillReminderService {

    private static final Logger log = LoggerFactory.getLogger(BillReminderService.class);

    private final BillReminderRepository billReminderRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    /**
     * Create a new bill reminder
     */
    public BillReminder createBillReminder(BillReminderDTO dto) {
        User currentUser = authService.getCurrentUser();
        
        BillReminder reminder = BillReminder.builder()
                .user(currentUser)
                .billName(dto.getBillName())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .dueDate(dto.getDueDate())
                .recurrencePattern(dto.getRecurrencePattern())
                .category(dto.getCategory())
                .paymentMethod(dto.getPaymentMethod())
                .status("ACTIVE")
                .reminderDays(dto.getReminderDays() != null ? dto.getReminderDays() : Arrays.asList(7, 3, 1))
                .isPaid(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        return billReminderRepository.save(reminder);
    }

    /**
     * Get all bill reminders for current user
     */
    public List<BillReminder> getUserBillReminders() {
        User currentUser = authService.getCurrentUser();
        return billReminderRepository.findByUserOrderByDueDateAsc(currentUser);
    }

    /**
     * Get upcoming bills (next 30 days)
     */
    public List<BillReminder> getUpcomingBills() {
        User currentUser = authService.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        
        return billReminderRepository.findByUserAndDueDateBetween(currentUser, today, thirtyDaysLater)
                .stream()
                .filter(reminder -> !reminder.isPaid())
                .collect(Collectors.toList());
    }

    /**
     * Get overdue bills
     */
    public List<BillReminder> getOverdueBills() {
        User currentUser = authService.getCurrentUser();
        LocalDate today = LocalDate.now();
        
        return billReminderRepository.findByUserAndDueDateBeforeAndIsPaidFalse(currentUser, today);
    }

    /**
     * Update bill reminder
     */
    public BillReminder updateBillReminder(Long id, BillReminderDTO dto) {
        BillReminder existingReminder = getBillReminder(id);
        
        existingReminder.setBillName(dto.getBillName());
        existingReminder.setDescription(dto.getDescription());
        existingReminder.setAmount(dto.getAmount());
        existingReminder.setDueDate(dto.getDueDate());
        existingReminder.setRecurrencePattern(dto.getRecurrencePattern());
        existingReminder.setCategory(dto.getCategory());
        existingReminder.setPaymentMethod(dto.getPaymentMethod());
        existingReminder.setReminderDays(dto.getReminderDays());
        existingReminder.setUpdatedAt(LocalDateTime.now());
        
        return billReminderRepository.save(existingReminder);
    }

    /**
     * Mark bill as paid
     */
    public BillReminder markAsPaid(Long id) {
        BillReminder reminder = getBillReminder(id);
        
        reminder.setPaid(true);
        reminder.setPaidDate(LocalDate.now());
        reminder.setStatus("PAID");
        reminder.setUpdatedAt(LocalDateTime.now());
        
        // Create next occurrence if recurring
        if (reminder.getRecurrencePattern() != null && !reminder.getRecurrencePattern().equals("NONE")) {
            createNextOccurrence(reminder);
        }
        
        return billReminderRepository.save(reminder);
    }

    /**
     * Delete bill reminder
     */
    public void deleteBillReminder(Long id) {
        BillReminder reminder = getBillReminder(id);
        billReminderRepository.delete(reminder);
    }

    /**
     * Get bill reminder by ID
     */
    public BillReminder getBillReminder(Long id) {
        User currentUser = authService.getCurrentUser();
        BillReminder reminder = billReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill reminder not found"));
        
        // Verify ownership
        if (!reminder.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return reminder;
    }

    /**
     * Create next occurrence of recurring bill
     */
    private void createNextOccurrence(BillReminder paidReminder) {
        LocalDate nextDueDate = calculateNextDueDate(paidReminder.getDueDate(), paidReminder.getRecurrencePattern());
        
        BillReminder nextReminder = BillReminder.builder()
                .user(paidReminder.getUser())
                .billName(paidReminder.getBillName())
                .description(paidReminder.getDescription())
                .amount(paidReminder.getAmount())
                .dueDate(nextDueDate)
                .recurrencePattern(paidReminder.getRecurrencePattern())
                .category(paidReminder.getCategory())
                .paymentMethod(paidReminder.getPaymentMethod())
                .reminderDays(paidReminder.getReminderDays())
                .status("ACTIVE")
                .isPaid(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        billReminderRepository.save(nextReminder);
        log.info("Created next occurrence for bill: {}", paidReminder.getBillName());
    }

    /**
     * Calculate next due date based on recurrence pattern
     */
    private LocalDate calculateNextDueDate(LocalDate currentDueDate, String pattern) {
        switch (pattern) {
            case "DAILY":
                return currentDueDate.plusDays(1);
            case "WEEKLY":
                return currentDueDate.plusWeeks(1);
            case "BIWEEKLY":
                return currentDueDate.plusWeeks(2);
            case "MONTHLY":
                return currentDueDate.plusMonths(1);
            case "BIMONTHLY":
                return currentDueDate.plusMonths(2);
            case "QUARTERLY":
                return currentDueDate.plusMonths(3);
            case "SEMIANNUALLY":
                return currentDueDate.plusMonths(6);
            case "YEARLY":
                return currentDueDate.plusYears(1);
            default:
                return currentDueDate;
        }
    }

    /**
     * Scheduled task to check and send reminders
     */
    @Scheduled(cron = "0 0 8 * * ?") // Run daily at 8 AM
    public void checkAndSendReminders() {
        log.info("Running bill reminder check...");
        
        LocalDate today = LocalDate.now();
        
        // Get all active reminders
        List<BillReminder> allReminders = billReminderRepository.findByStatus("ACTIVE");
        
        for (BillReminder reminder : allReminders) {
            if (reminder.isPaid()) {
                continue;
            }
            
            // Check if reminder should be sent today
            long daysUntilDue = ChronoUnit.DAYS.between(today, reminder.getDueDate());
            
            if (reminder.getReminderDays() != null && reminder.getReminderDays().contains((int) daysUntilDue)) {
                sendReminder(reminder, daysUntilDue);
            }
            
            // Check for overdue bills
            if (daysUntilDue < 0) {
                sendOverdueReminder(reminder, Math.abs(daysUntilDue));
            }
        }
        
        log.info("Bill reminder check completed");
    }

    /**
     * Send reminder notification
     */
    private void sendReminder(BillReminder reminder, long daysUntilDue) {
        String message;
        if (daysUntilDue == 0) {
            message = String.format("Your bill '%s' is due today! Amount: Rs %.2f", 
                reminder.getBillName(), reminder.getAmount());
        } else if (daysUntilDue == 1) {
            message = String.format("Your bill '%s' is due tomorrow! Amount: Rs %.2f", 
                reminder.getBillName(), reminder.getAmount());
        } else {
            message = String.format("Your bill '%s' is due in %d days! Amount: Rs %.2f", 
                reminder.getBillName(), daysUntilDue, reminder.getAmount());
        }
        
        notificationService.sendNotification(reminder.getUser(), "Bill Reminder", message);
        log.info("Sent reminder for bill: {}", reminder.getBillName());
    }

    /**
     * Send overdue reminder
     */
    private void sendOverdueReminder(BillReminder reminder, long daysOverdue) {
        String message = String.format("Your bill '%s' is overdue by %d days! Amount: Rs %.2f. Please pay ASAP.", 
            reminder.getBillName(), daysOverdue, reminder.getAmount());
        
        notificationService.sendNotification(reminder.getUser(), "Overdue Bill", message);
        log.warn("Sent overdue reminder for bill: {}", reminder.getBillName());
    }

    /**
     * Get bill summary statistics
     */
    public Map<String, Object> getBillSummary() {
        User currentUser = authService.getCurrentUser();
        List<BillReminder> userBills = billReminderRepository.findByUser(currentUser);
        
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        LocalDate ninetyDaysLater = today.plusDays(90);
        
        // Calculate statistics
        long totalBills = userBills.size();
        long paidBills = userBills.stream().filter(BillReminder::isPaid).count();
        long unpaidBills = totalBills - paidBills;
        
        List<BillReminder> overdueBills = userBills.stream()
                .filter(bill -> !bill.isPaid() && bill.getDueDate().isBefore(today))
                .collect(Collectors.toList());
        
        List<BillReminder> upcomingBills = userBills.stream()
                .filter(bill -> !bill.isPaid() && 
                        !bill.getDueDate().isBefore(today) && 
                        !bill.getDueDate().isAfter(thirtyDaysLater))
                .collect(Collectors.toList());
        
        BigDecimal totalAmountDue = upcomingBills.stream()
                .map(BillReminder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalOverdueAmount = overdueBills.stream()
                .map(BillReminder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Monthly breakdown
        Map<String, BigDecimal> monthlyBreakdown = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            LocalDate monthStart = today.plusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            BigDecimal monthTotal = userBills.stream()
                    .filter(bill -> !bill.getDueDate().isBefore(monthStart) && 
                                   !bill.getDueDate().isAfter(monthEnd))
                    .map(BillReminder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            monthlyBreakdown.put(monthStart.getMonth().toString(), monthTotal);
        }
        
        return Map.of(
            "totalBills", totalBills,
            "paidBills", paidBills,
            "unpaidBills", unpaidBills,
            "overdueBills", overdueBills.size(),
            "upcomingBills", upcomingBills.size(),
            "totalAmountDue", totalAmountDue,
            "totalOverdueAmount", totalOverdueAmount,
            "monthlyBreakdown", monthlyBreakdown,
            "paymentRate", totalBills > 0 ? (double) paidBills / totalBills : 0.0
        );
    }

    /**
     * Get bills by category
     */
    public Map<String, Object> getBillsByCategory() {
        User currentUser = authService.getCurrentUser();
        List<BillReminder> userBills = billReminderRepository.findByUser(currentUser);
        
        Map<String, List<BillReminder>> categoryGroups = userBills.stream()
                .collect(Collectors.groupingBy(bill -> bill.getCategory() != null ? bill.getCategory() : "Other"));
        
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, List<BillReminder>> entry : categoryGroups.entrySet()) {
            List<BillReminder> categoryBills = entry.getValue();
            BigDecimal totalAmount = categoryBills.stream()
                    .map(BillReminder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long paidCount = categoryBills.stream().filter(BillReminder::isPaid).count();
            
            result.put(entry.getKey(), Map.of(
                "count", categoryBills.size(),
                "totalAmount", totalAmount,
                "paidCount", paidCount,
                "unpaidCount", categoryBills.size() - paidCount
            ));
        }
        
        return result;
    }

    /**
     * Bulk update bill reminders
     */
    public List<BillReminder> bulkUpdate(List<Long> ids, BillReminderDTO dto) {
        List<BillReminder> updatedReminders = new ArrayList<>();
        
        for (Long id : ids) {
            try {
                BillReminder reminder = getBillReminder(id);
                reminder.setBillName(dto.getBillName());
                reminder.setAmount(dto.getAmount());
                reminder.setRecurrencePattern(dto.getRecurrencePattern());
                reminder.setCategory(dto.getCategory());
                reminder.setPaymentMethod(dto.getPaymentMethod());
                reminder.setUpdatedAt(LocalDateTime.now());
                
                updatedReminders.add(billReminderRepository.save(reminder));
            } catch (Exception e) {
                log.error("Error updating bill reminder with ID: {}", id, e);
            }
        }
        
        return updatedReminders;
    }

    /**
     * Get bills due in specific date range
     */
    public List<BillReminder> getBillsDueBetween(LocalDate startDate, LocalDate endDate) {
        User currentUser = authService.getCurrentUser();
        return billReminderRepository.findByUserAndDueDateBetween(currentUser, startDate, endDate);
    }
}
