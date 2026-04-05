package com.expense.controller;

import com.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ExpenseService expenseService;

    @PostMapping("/csv")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
        expenseService.processCsv(file);
        return ResponseEntity.ok("CSV processed successfully");
    }

    @PostMapping("/receipt")
    public ResponseEntity<String> uploadReceipt(@RequestParam("file") MultipartFile file) {
        expenseService.processReceiptOcr(file);
        return ResponseEntity.ok("Receipt processed");
    }

    @PostMapping("/sms")
    public ResponseEntity<String> uploadSms(@RequestBody String smsText) {
        expenseService.processSms(smsText);
        return ResponseEntity.ok("SMS processed");
    }
}