package com.sudheer.portfoliotracker.infrastructure.scheduler;

import com.sudheer.portfoliotracker.application.usecase.SyncAmfiDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyNavScheduler {

    private final SyncAmfiDataUseCase syncAmfiDataUseCase;

    @Scheduled(cron = "${amfi.daily.cron}", zone = "Asia/Kolkata")
    public void runDailyNavSync() throws Exception {
        syncAmfiDataUseCase.runDaily();
    }
}