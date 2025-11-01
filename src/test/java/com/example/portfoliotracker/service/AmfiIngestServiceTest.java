package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmfiIngestServiceTest {

    private RestTemplate restTemplate;
    private AmfiSchemeRepository schemeRepository;
    private AmfiNavRepository navRepository;
    private AmfiIngestService ingestService;

    @Captor
    ArgumentCaptor<AmfiNav> navCaptor = ArgumentCaptor.forClass(AmfiNav.class);

    @BeforeEach
    void setup() {
        restTemplate = mock(RestTemplate.class);
        schemeRepository = mock(AmfiSchemeRepository.class);
        navRepository = mock(AmfiNavRepository.class);
        ingestService = new AmfiIngestService(restTemplate, schemeRepository, navRepository, "http://dummy");
    }

    @Test
    void parsesLineWithHeader_andSavesSchemeAndNav() {
        String header = "Scheme Code;ISIN Div Payout/ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date";
        String line = "12345;INEAAA000000;INEBBB000000;Example Scheme;12.34;01-Jan-2020";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(header + "\n" + line + "\n");
        when(schemeRepository.findBySchemeCode("12345")).thenReturn(Optional.empty());
        when(navRepository.findBySchemeCodeAndNavDate(eq("12345"), any(LocalDate.class))).thenReturn(Optional.empty());

        ingestService.fetchAndIngest();

        verify(schemeRepository, atLeastOnce()).save(any(AmfiScheme.class));
        verify(navRepository).save(navCaptor.capture());
        AmfiNav savedNav = navCaptor.getValue();
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

        verify(schemeRepository, atLeastOnce()).save(any(AmfiScheme.class));
        verify(navRepository, never()).save(any(AmfiNav.class));
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

        verify(schemeRepository, atLeastOnce()).save(any(AmfiScheme.class));
        verify(navRepository, never()).save(argThat(nav -> "99999".equals(nav.getSchemeCode())));
    }
}

