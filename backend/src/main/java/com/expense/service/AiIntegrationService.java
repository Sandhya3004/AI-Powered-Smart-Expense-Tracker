package com.expense.service;

import com.expense.dto.PredictionDTO;
import com.expense.entity.Expense;
import com.expense.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIntegrationService {

    @Value("${ai.service.url}")
    private String aiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    public List<Expense> detectAnomalies() {
        try {
            Long userId = authService.getCurrentUser().getId();
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDate, endDate);

            List<Double> amounts = expenses.stream()
                    .map(e -> e.getAmount().doubleValue())
                    .collect(Collectors.toList());

            if (amounts.isEmpty()) {
                return Collections.emptyList();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amounts", amounts);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = aiBaseUrl + "/anomaly";
            JsonNode response = restTemplate.postForObject(url, request, JsonNode.class);

            List<Integer> anomalyIndices = new ArrayList<>();
            if (response != null && response.has("anomaly_indices")) {
                response.get("anomaly_indices").forEach(index -> anomalyIndices.add(index.asInt()));
            }

            return anomalyIndices.stream()
                    .map(expenses::get)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("AI anomaly detection failed: {}", e.getMessage());
            return Collections.emptyList(); // Fallback: no anomalies detected
        }
    }

    public PredictionDTO getPrediction() {
        try {
            Long userId = authService.getCurrentUser().getId();
            List<Double> monthlyTotals = new ArrayList<>();
            int monthsToFetch = 6;
            LocalDate now = LocalDate.now();
            for (int i = monthsToFetch; i > 0; i--) {
                YearMonth month = YearMonth.from(now.minusMonths(i));
                LocalDate start = month.atDay(1);
                LocalDate end = month.atEndOfMonth();
                List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
                BigDecimal total = expenses.stream()
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                monthlyTotals.add(total.doubleValue());
            }

            if (monthlyTotals.stream().allMatch(t -> t == 0.0)) {
                PredictionDTO dto = new PredictionDTO();
                dto.setPredictedAmount(null);
                dto.setMessage("Insufficient historical data for prediction");
                return dto;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("monthly_totals", monthlyTotals);
            requestBody.put("last_month_index", monthsToFetch - 1);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = aiBaseUrl + "/predict";
            JsonNode response = restTemplate.postForObject(url, request, JsonNode.class);

            PredictionDTO dto = new PredictionDTO();
            if (response != null && response.has("predicted_amount")) {
                dto.setPredictedAmount(BigDecimal.valueOf(response.get("predicted_amount").asDouble()));
                dto.setMessage(response.get("message").asText());
            } else {
                dto.setPredictedAmount(null);
                dto.setMessage("Prediction service unavailable");
            }
            return dto;
        } catch (Exception e) {
            log.error("AI prediction failed: {}", e.getMessage());
            PredictionDTO dto = new PredictionDTO();
            dto.setPredictedAmount(null);
            dto.setMessage("Prediction service unavailable");
            return dto;
        }
    }

    public String categorizeExpense(String description) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Collections.singletonMap("description", description);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            String url = aiBaseUrl + "/categorize";
            JsonNode response = restTemplate.postForObject(url, request, JsonNode.class);
            return response != null && response.has("category") ? response.get("category").asText() : "Other";
        } catch (Exception e) {
            log.error("AI service categorization failed, using fallback: {}", e.getMessage());
            return "Other"; // Fallback category
        }
    }

    public String processOcr(byte[] image) {
        // This method should call the AI microservice's /ocr endpoint.
        // For now, stub – you'll need to implement using RestTemplate with file upload.
        return "OCR stub";
    }

    public Map<String, Object> parseOcrResult(String ocrText) {
        // Parse the raw OCR text into structured data.
        // This is a simple example; improve as needed.
        Map<String, Object> result = new HashMap<>();
        // Use regex to extract amount, merchant, date from ocrText
        // For now, return dummy data:
        result.put("amount", 0.0);
        result.put("merchant", "Unknown");
        result.put("date", LocalDate.now().toString());
        return result;
    }
}