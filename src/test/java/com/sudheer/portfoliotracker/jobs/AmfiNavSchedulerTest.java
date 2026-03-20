package com.sudheer.portfoliotracker.jobs;

import com.sudheer.portfoliotracker.application.usecase.SyncAmfiDataUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AmfiNavSchedulerTest {

    @Test
    void runDaily_callsSyncUseCase() throws Exception {
        SyncAmfiDataUseCase syncUseCase = mock(SyncAmfiDataUseCase.class);
        AmfiNavScheduler scheduler = new AmfiNavScheduler(syncUseCase);

        scheduler.runDaily();

        verify(syncUseCase, times(1)).runDaily();
    }
}

