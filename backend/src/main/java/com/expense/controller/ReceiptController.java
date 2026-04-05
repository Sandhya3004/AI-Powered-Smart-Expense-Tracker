package com.expense.controller;

import com.expense.entity.Receipt;
import com.expense.service.ReceiptProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptProcessingService receiptProcessingService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Receipt>> uploadReceipt(@RequestParam("file") MultipartFile file) {
        try {
            Receipt receipt = receiptProcessingService.processReceipt(file);
            return ResponseEntity.ok(Map.of("receipt", receipt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my")   // distinct from admin's root
    public ResponseEntity<List<Receipt>> getUserReceipts() {
        List<Receipt> receipts = receiptProcessingService.getUserReceipts();
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/detail/{id}")   // changed from /{id}/detail to avoid any possible conflict
    public ResponseEntity<Receipt> getReceipt(@PathVariable Long id) {
        try {
            Receipt receipt = receiptProcessingService.getReceipt(id);
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{id}")   // explicit path
    public ResponseEntity<Void> deleteReceipt(@PathVariable Long id) {
        try {
            receiptProcessingService.deleteReceipt(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/unprocessed")
    public ResponseEntity<List<Receipt>> getUnprocessedReceipts() {
        // Implement in service
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/reprocess/{id}")
    public ResponseEntity<Receipt> reprocessReceipt(@PathVariable Long id) {
        // Implement in service
        return ResponseEntity.ok().build();
    }

    @GetMapping("/high-confidence")
    public ResponseEntity<List<Receipt>> getHighConfidenceReceipts() {
        // Implement in service
        return ResponseEntity.ok(List.of());
    }
}