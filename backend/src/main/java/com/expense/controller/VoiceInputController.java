package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.dto.VoiceInputDTO;
import com.expense.entity.Expense;
import com.expense.service.ExpenseService;
import com.expense.service.VoiceInputService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceInputController {

    private final VoiceInputService voiceInputService;
    private final ExpenseService expenseService;

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribeAudio(@RequestBody Map<String, String> request) {
        try {
            String audioData = request.get("audioData");
            String transcription = voiceInputService.transcribeAudio(audioData);
            
            Map<String, Object> response = Map.of(
                "transcription", transcription,
                "success", true
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "transcription", "",
                "success", false,
                "error", "Failed to transcribe audio"
            );
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processVoice(@RequestBody Map<String, String> request) {
        try {
            String transcription = request.get("transcription");
            if (transcription == null || transcription.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Transcript is required"
                ));
            }

            VoiceInputDTO voiceInputDTO = VoiceInputDTO.builder()
                    .transcript(transcription)
                    .build();

            Expense createdExpense = voiceInputService.processVoiceInput(voiceInputDTO);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Expense created successfully from voice input",
                "expense", createdExpense
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to process voice input: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/process-expense")
    public ResponseEntity<Map<String, Object>> processExpenseFromVoice(@RequestBody Map<String, String> request) {
        try {
            String transcription = request.get("transcription");
            Map<String, Object> expenseData = voiceInputService.parseExpenseFromText(transcription);

            return ResponseEntity.ok(Map.of(
                "expenseData", expenseData,
                "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "expenseData", Map.of(),
                "success", false,
                "error", "Failed to process expense from voice input"
            ));
        }
    }
}
