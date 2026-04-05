package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.dto.BillReminderDTO;
import com.expense.entity.BillReminder;
import com.expense.service.BillReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bill-reminders")
@RequiredArgsConstructor
public class BillReminderController extends BaseController {

    private final BillReminderService billReminderService;

    @PostMapping
    public ResponseEntity<ApiResponse<BillReminder>> createBillReminder(@RequestBody BillReminderDTO dto) {
        try {
            BillReminder reminder = billReminderService.createBillReminder(dto);
            return ResponseEntity.ok(ApiResponse.success(reminder, "Bill created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create bill: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillReminder>>> getUserBillReminders() {
        try {
            List<BillReminder> reminders = billReminderService.getUserBillReminders();
            return ResponseEntity.ok(ApiResponse.success(reminders, "Bills retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch bills: " + e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<BillReminder>>> getUpcomingBills() {
        try {
            List<BillReminder> upcomingBills = billReminderService.getUpcomingBills();
            return ResponseEntity.ok(ApiResponse.success(upcomingBills, "Upcoming bills retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch upcoming bills: " + e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<BillReminder>>> getOverdueBills() {
        try {
            List<BillReminder> overdueBills = billReminderService.getOverdueBills();
            return ResponseEntity.ok(ApiResponse.success(overdueBills, "Overdue bills retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch overdue bills: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillReminder>> getBillReminder(@PathVariable Long id) {
        try {
            BillReminder reminder = billReminderService.getBillReminder(id);
            return ResponseEntity.ok(ApiResponse.success(reminder, "Bill retrieved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch bill: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BillReminder>> updateBillReminder(
            @PathVariable Long id, 
            @RequestBody BillReminderDTO dto) {
        try {
            BillReminder reminder = billReminderService.updateBillReminder(id, dto);
            return ResponseEntity.ok(ApiResponse.success(reminder, "Bill updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update bill: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBillReminder(@PathVariable Long id) {
        try {
            billReminderService.deleteBillReminder(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Bill deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete bill: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<BillReminder>> markAsPaid(@PathVariable Long id) {
        try {
            BillReminder reminder = billReminderService.markAsPaid(id);
            return ResponseEntity.ok(ApiResponse.success(reminder, "Bill marked as paid successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to mark bill as paid: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBillSummary() {
        try {
            Map<String, Object> summary = billReminderService.getBillSummary();
            return ResponseEntity.ok(ApiResponse.success(summary, "Bill summary retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch bill summary: " + e.getMessage()));
        }
    }

    @GetMapping("/category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBillsByCategory() {
        try {
            Map<String, Object> categoryBreakdown = billReminderService.getBillsByCategory();
            return ResponseEntity.ok(ApiResponse.success(categoryBreakdown, "Bills by category retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch bills by category: " + e.getMessage()));
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<BillReminder>>> getBillsDueBetween(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        try {
            List<BillReminder> bills = billReminderService.getBillsDueBetween(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(bills, "Bills in date range retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch bills: " + e.getMessage()));
        }
    }
}
