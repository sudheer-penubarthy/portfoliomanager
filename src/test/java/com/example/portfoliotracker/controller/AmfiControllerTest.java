package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import com.example.portfoliotracker.repository.FundHouseRepository;
import com.example.portfoliotracker.service.AmfiIngestService;
import com.example.portfoliotracker.service.AmfiService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AmfiControllerTest {

    @Test
    void ingestEndpoint_invokesService() {
        AmfiIngestService ingestService = mock(AmfiIngestService.class);
        AmfiService amfiService = mock(AmfiService.class);
        FundHouseRepository fundHouseRepository = mock(FundHouseRepository.class);
        AmfiSchemeRepository schemeRepository = mock(AmfiSchemeRepository.class);

        AmfiController controller = new AmfiController(amfiService, ingestService, fundHouseRepository, schemeRepository);

        controller.triggerSync();

        verify(ingestService, times(1)).fetchAndIngestAsync();
    }
}
