package com.expense.controller;

import com.expense.service.VoiceTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceTransactionController {

    private final VoiceTransactionService voiceTransactionService;

    @PostMapping("/process-text")
    public ResponseEntity<Map<String, Object>> processVoiceCommand(@RequestBody Map<String, String> request) {
        String voiceText = request.get("voiceText");
        Map<String, Object> result = voiceTransactionService.processVoiceCommand(voiceText);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/examples")
    public ResponseEntity<Map<String, Object>> getVoiceCommandExamples() {
        Map<String, Object> examples = voiceTransactionService.getVoiceCommandExamples();
        return ResponseEntity.ok(examples);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateVoiceCommand(@RequestBody Map<String, String> request) {
        String voiceText = request.get("voiceText");
        Map<String, Object> validation = voiceTransactionService.validateVoiceCommand(voiceText);
        return ResponseEntity.ok(validation);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getVoiceCommandSuggestions(@RequestParam String partialText) {
        List<String> suggestions = voiceTransactionService.getVoiceCommandSuggestions(partialText);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/commands")
    public ResponseEntity<List<String>> getSupportedCommands() {
        List<String> commands = List.of(
            "spent", "paid", "bought", "purchased", "expense",
            "received", "earned", "got", "income", "salary", "bonus",
            "transferred", "moved", "sent", "transfer"
        );
        return ResponseEntity.ok(commands);
    }

    @GetMapping("/patterns")
    public ResponseEntity<Map<String, Object>> getVoicePatterns() {
        Map<String, Object> patterns = Map.of(
            "amount_patterns", List.of("Rs 500", "500 rupees", "₹500", "500.00"),
            "merchant_patterns", List.of("at merchant", "from store", "in shop"),
            "date_patterns", List.of("on 15/03/2024", "today", "yesterday"),
            "category_patterns", List.of("for food", "category transport", "for shopping")
        );
        return ResponseEntity.ok(patterns);
    }
}
