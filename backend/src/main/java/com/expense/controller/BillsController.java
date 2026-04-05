package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.dto.BillDTO;
import com.expense.entity.Bill;
import com.expense.entity.User;
import com.expense.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Slf4j
public class BillsController extends BaseController {

    private final BillService billService;

    @PostMapping
    public ResponseEntity<ApiResponse<BillDTO>> createBill(@RequestBody Bill bill) {
        try {
            User currentUser = getCurrentUser();
            bill.setUser(currentUser);
            Bill createdBill = billService.createBill(bill);
            return ResponseEntity.ok(ApiResponse.success(mapToDTO(createdBill), "Bill created successfully"));
        } catch (Exception e) {
            log.error("Error creating bill", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to create bill: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillDTO>>> getAllBills() {
        try {
            User currentUser = getCurrentUser();
            List<Bill> bills = billService.getUserBills(currentUser);
            // Map to DTOs to avoid LazyInitializationException
            List<BillDTO> billDTOs = bills.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(billDTOs, "Bills retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching bills", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch bills: " + e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<BillDTO>>> getUpcomingBills(
            @RequestParam(defaultValue = "7") int daysAhead) {
        try {
            User currentUser = getCurrentUser();
            List<Bill> bills = billService.getUpcomingBills(currentUser, daysAhead);
            List<BillDTO> billDTOs = bills.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(billDTOs, "Upcoming bills retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching upcoming bills", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch upcoming bills: " + e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<BillDTO>>> getOverdueBills() {
        try {
            User currentUser = getCurrentUser();
            List<Bill> bills = billService.getUserBills(currentUser);
            List<Bill> overdueBills = bills.stream()
                    .filter(bill -> !bill.isPaid() && bill.getDueDate().isBefore(LocalDate.now()))
                    .toList();
            List<BillDTO> billDTOs = overdueBills.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(billDTOs, "Overdue bills retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching overdue bills", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch overdue bills: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BillDTO>> updateBill(
            @PathVariable Long id,
            @RequestBody Bill bill) {
        try {
            User currentUser = getCurrentUser();
            Bill existingBill = billService.getBillById(id);
            
            if (existingBill == null || !existingBill.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            bill.setId(id);
            bill.setUser(currentUser);
            Bill updatedBill = billService.updateBill(bill);
            return ResponseEntity.ok(ApiResponse.success(mapToDTO(updatedBill), "Bill updated successfully"));
        } catch (Exception e) {
            log.error("Error updating bill", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update bill: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBill(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Bill existingBill = billService.getBillById(id);
            
            if (existingBill == null || !existingBill.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            billService.deleteBill(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Bill deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting bill", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to delete bill: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<BillDTO>> markAsPaid(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Bill existingBill = billService.getBillById(id);
            
            if (existingBill == null || !existingBill.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            Bill updatedBill = billService.markBillAsPaid(id);
            return ResponseEntity.ok(ApiResponse.success(mapToDTO(updatedBill), "Bill marked as paid successfully"));
        } catch (Exception e) {
            log.error("Error marking bill as paid", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to mark bill as paid: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBillsSummary() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> summary = billService.getBillsSummary(currentUser);
            return ResponseEntity.ok(ApiResponse.success(summary, "Bills summary retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching bills summary", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch bills summary: " + e.getMessage()));
        }
    }

    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<ApiResponse<List<BillDTO>>> getBillsForMonth(
            @PathVariable int year,
            @PathVariable int month) {
        try {
            User currentUser = getCurrentUser();
            List<Bill> allBills = billService.getUserBills(currentUser);
            List<Bill> monthBills = allBills.stream()
                    .filter(bill -> {
                        LocalDate dueDate = bill.getDueDate();
                        return dueDate.getYear() == year && dueDate.getMonthValue() == month;
                    })
                    .toList();
            List<BillDTO> billDTOs = monthBills.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(billDTOs, "Bills for month retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching bills for month", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch bills for month: " + e.getMessage()));
        }
    }

    private BillDTO mapToDTO(Bill bill) {
        String status;
        if (bill.isPaid()) {
            status = "PAID";
        } else if (bill.getDueDate().isBefore(LocalDate.now())) {
            status = "OVERDUE";
        } else {
            status = "PENDING";
        }

        return BillDTO.builder()
                .id(bill.getId())
                .title(bill.getTitle())
                .description(bill.getDescription())
                .amount(bill.getAmount())
                .dueDate(bill.getDueDate())
                .category(bill.getCategory())
                .paid(bill.isPaid())
                .status(status)
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }
}
