package com.example.portfoliotracker.jobs;

import com.example.portfoliotracker.service.AmfiIngestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AmfiNavScheduler {
    private final AmfiIngestService ingestService;

    public AmfiNavScheduler(AmfiIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @Scheduled(cron = "${amfi.nav.cron}")
    public void runDailyIngest() {
        ingestService.fetchAndIngest();
    }
}
