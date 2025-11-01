package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.service.AmfiIngestService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AmfiControllerTest {

    @Test
    void ingestEndpoint_invokesService() {
        AmfiIngestService ingestService = mock(AmfiIngestService.class);
        AmfiController controller = new AmfiController(ingestService);

        controller.ingest();

        verify(ingestService, times(1)).fetchAndIngest();
    }
}

