package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.Receipt;
import com.expense.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/receipts")   // clearly separate base path
@RequiredArgsConstructor
public class ReceiptsController {

    private final ReceiptService receiptService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "expenseId", required = false) Long expenseId) {
        try {
            Map<String, Object> result = receiptService.uploadReceipt(file, expenseId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Failed to upload receipt: " + e.getMessage()
            );
            return ResponseEntity.ok(error);
        }
    }

    @GetMapping("/all")   // changed from root to /all to avoid any possible root‑level conflict
    public ResponseEntity<List<Receipt>> getAllReceipts() {
        try {
            List<Receipt> receipts = receiptService.getAllReceipts();
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch receipts", e);
        }
    }

    @GetMapping("/details/{id}")   // changed from /{id} to /details/{id}
    public ResponseEntity<Receipt> getReceipt(@PathVariable Long id) {
        try {
            Receipt receipt = receiptService.getReceiptById(id);
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch receipt", e);
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        try {
            byte[] imageData = receiptService.downloadReceipt(id);
            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .header("Content-Disposition", "attachment; filename=receipt_" + id + ".jpg")
                    .body(imageData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download receipt", e);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReceipt(@PathVariable Long id) {
        try {
            receiptService.deleteReceipt(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Receipt deleted successfully"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete receipt", e);
        }
    }

    @PostMapping("/process/{id}")   // changed from /{id}/process to /process/{id}
    public ResponseEntity<Map<String, Object>> processReceipt(@PathVariable Long id) {
        try {
            Map<String, Object> extractedData = receiptService.processReceipt(id);
            return ResponseEntity.ok(extractedData);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Failed to process receipt: " + e.getMessage()
            );
            return ResponseEntity.ok(error);
        }
    }

    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> extractTextFromReceipt(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> extractedData = receiptService.extractTextFromImage(file);
            return ResponseEntity.ok(extractedData);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Failed to extract text from receipt: " + e.getMessage()
            );
            return ResponseEntity.ok(error);
        }
    }

    @GetMapping("/expense/{expenseId}")
    public ResponseEntity<List<Receipt>> getReceiptsByExpense(@PathVariable Long expenseId) {
        try {
            List<Receipt> receipts = receiptService.getReceiptsByExpense(expenseId);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch receipts for expense", e);
        }
    }

    @PostMapping("/{id}/link-expense")
    public ResponseEntity<ApiResponse<Void>> linkReceiptToExpense(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long expenseId = Long.valueOf(request.get("expenseId").toString());
            receiptService.linkReceiptToExpense(id, expenseId);
            return ResponseEntity.ok(ApiResponse.success(null, "Receipt linked to expense successfully"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to link receipt to expense", e);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Receipt>> searchReceipts(@RequestParam String query) {
        try {
            List<Receipt> receipts = receiptService.searchReceipts(query);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search receipts", e);
        }
    }
}