package com.example.portfoliotracker.jobs;

import com.example.portfoliotracker.service.AmfiIngestService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AmfiNavSchedulerTest {

    @Test
    void runDailyIngest_callsIngestService() {
        AmfiIngestService ingestService = mock(AmfiIngestService.class);
        AmfiNavScheduler scheduler = new AmfiNavScheduler(ingestService);

        scheduler.runDailyIngest();

        verify(ingestService, times(1)).fetchAndIngest();
    }
}

