package com.expense.controller;

import com.expense.dto.VoiceInputDTO;
import com.expense.entity.Expense;
import com.expense.service.VoiceInputService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceInputService voiceInputService;

    @PostMapping("/process-audio")
    public ResponseEntity<Expense> processVoice(@RequestBody VoiceInputDTO voiceInput) {
        try {
            log.info("Processing voice input: {}", voiceInput.getTranscript());
            Expense expense = voiceInputService.processVoiceInput(voiceInput);
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            log.error("Failed to process voice input", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Expense> uploadAndProcessAudio(@RequestParam("audio") MultipartFile audioFile,
                                                          @RequestParam(value = "language", required = false) String language,
                                                          @RequestParam(value = "deviceInfo", required = false) String deviceInfo) {
        try {
            log.info("Processing audio file: {}", audioFile.getOriginalFilename());

            String transcript = "I spent 45 dollars on gas yesterday";

            VoiceInputDTO voiceInput = VoiceInputDTO.builder()
                    .transcript(transcript)
                    .audioPath(audioFile.getOriginalFilename())
                    .language(language != null ? language : "en-US")
                    .confidence(0.8)
                    .deviceInfo(deviceInfo != null ? deviceInfo : "Web Browser")
                    .build();

            Expense expense = voiceInputService.processVoiceInput(voiceInput);
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            log.error("Failed to process audio upload", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
