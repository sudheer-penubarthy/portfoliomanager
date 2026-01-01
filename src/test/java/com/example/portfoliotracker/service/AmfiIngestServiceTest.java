package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.entity.AmfiImport;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import com.example.portfoliotracker.repository.FundHouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmfiIngestServiceTest {

    private RestTemplate restTemplate;
    private AmfiSchemeRepository schemeRepository;
    private AmfiNavRepository navRepository;
    private AmfiIngestService ingestService;
    private FundHouseRepository fundHouseRepository;
    private AmfiPersistService persistService;
    private AmfiImportRepository importRepository;

    @Captor
    ArgumentCaptor<List> navListCaptor = ArgumentCaptor.forClass(List.class);

    @BeforeEach
    void setup() {
        restTemplate = mock(RestTemplate.class);
        schemeRepository = mock(AmfiSchemeRepository.class);
        navRepository = mock(AmfiNavRepository.class);
        fundHouseRepository = mock(FundHouseRepository.class);
        persistService = mock(AmfiPersistService.class);
        importRepository = mock(AmfiImportRepository.class);
        // Ensure importRepository.save(...) returns an entity with an ID to avoid NPE in fetchAndIngest
        when(importRepository.save(any(AmfiImport.class))).thenAnswer(invocation -> {
            AmfiImport arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });
        ingestService = new AmfiIngestService(restTemplate, schemeRepository, navRepository, fundHouseRepository, persistService, "http://dummy", importRepository);
    }

    @Test
    void parsesLineWithHeader_andSavesSchemeAndNav() {
        String header = "Scheme Code;ISIN Div Payout/ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date";
        String line = "12345;INEAAA000000;INEBBB000000;Example Scheme;12.34;01-Jan-2020";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(header + "\n" + line + "\n");
        when(schemeRepository.findBySchemeCode("12345")).thenReturn(Optional.empty());
        when(navRepository.findBySchemeCodeAndNavDate(eq("12345"), any(LocalDate.class))).thenReturn(Optional.empty());

        ingestService.fetchAndIngest();

        verify(persistService, atLeastOnce()).persistSchemesChunk(anyList());
        verify(persistService, atLeastOnce()).persistNavsJdbcChunk(navListCaptor.capture());
        List captured = navListCaptor.getValue();
        assertNotNull(captured);
        assertFalse(captured.isEmpty());
        Object first = captured.get(0);
        assertTrue(first instanceof AmfiNav);
        AmfiNav savedNav = (AmfiNav) first;
        assertEquals("12345", savedNav.getSchemeCode());
        assertEquals(new BigDecimal("12.34"), savedNav.getNavValue());
        assertEquals(LocalDate.of(2020,1,1), savedNav.getNavDate());
    }

    @Test
    void malformedNav_skipsNavSave_butSavesScheme() {
        String header = "Scheme Code;ISIN Div Payout/ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date";
        String line = "54321;INECCC000000;INEDDD000000;Malformed Nav Scheme;ABC;";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(header + "\n" + line + "\n");
        when(schemeRepository.findBySchemeCode("54321")).thenReturn(Optional.empty());

        ingestService.fetchAndIngest();

        verify(persistService, atLeastOnce()).persistSchemesChunk(anyList());
        verify(persistService, never()).persistNavsJdbcChunk(anyList());
    }

    @Test
    void idempotent_skipsExistingNav() {
        String header = "Scheme Code;ISIN Div Payout/ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date";
        String line = "99999;INEZZZ000000;INEYYY000000;Idempotent Scheme;5.50;02-Jan-2020";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(header + "\n" + line + "\n");
        when(schemeRepository.findBySchemeCode("99999")).thenReturn(Optional.empty());
        when(navRepository.findBySchemeCodeAndNavDate(eq("99999"), eq(LocalDate.of(2020,1,2))))
                .thenReturn(Optional.of(new AmfiNav()));

        ingestService.fetchAndIngest();

        verify(persistService, atLeastOnce()).persistSchemesChunk(anyList());
        // capture any calls to persistNavsJdbcChunk and assert none contain a nav with scheme '99999' if any calls occurred
        try {
            verify(persistService, atLeastOnce()).persistNavsJdbcChunk(navListCaptor.capture());
        } catch (org.mockito.exceptions.verification.WantedButNotInvoked e) {
            // no navs persisted at all - that's acceptable
            return;
        }
        List<List> allCaptured = navListCaptor.getAllValues();
        boolean found = allCaptured.stream().flatMap(List::stream).anyMatch(o -> o instanceof AmfiNav && "99999".equals(((AmfiNav)o).getSchemeCode()));
        assertFalse(found, "No persisted nav should have scheme code 99999");
    }
}
