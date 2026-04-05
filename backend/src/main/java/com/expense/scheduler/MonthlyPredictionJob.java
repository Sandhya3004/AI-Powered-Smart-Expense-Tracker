package com.expense.scheduler;

import com.expense.service.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyPredictionJob {

    private final InsightService insightService;

    @Scheduled(cron = "0 0 1 1 * ?")
    public void generateMonthlyPredictions() {
        log.info("Starting monthly prediction generation for all users");
        // This would iterate over users and call insightService.getMonthlyPrediction()
        // then store or send alerts
        log.info("Monthly prediction job completed");
    }
}