package com.sudheer.portfoliotracker.service;

import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiNav;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiScheme;
import com.sudheer.portfoliotracker.repository.AmfiNavRepository;
import com.sudheer.portfoliotracker.repository.AmfiSchemeRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmfiServiceTest {

    @Test
    void findByFundHouse_returnsAllWhenFundHouseIsNullOrBlank() {
        AmfiSchemeRepository schemeRepo = mock(AmfiSchemeRepository.class);
        AmfiNavRepository navRepo = mock(AmfiNavRepository.class);
        var ingest = mock(com.sudheer.portfoliotracker.service.AmfiIngestService.class);

        AmfiScheme s1 = new AmfiScheme(); s1.setSchemeCode("1");
        when(schemeRepo.findAll()).thenReturn(List.of(s1));

        AmfiService svc = new AmfiService(schemeRepo, navRepo, ingest);

        List<AmfiScheme> resultNull = svc.findByFundHouse(null, true);
        List<AmfiScheme> resultBlank = svc.findByFundHouse("   ", false);

        assertEquals(1, resultNull.size());
        assertSame(s1, resultNull.get(0));
        assertEquals(1, resultBlank.size());
    }

    @Test
    void findByFundHouse_filtersByFundHouseAndRespectsActiveOnlyFlag() {
        AmfiSchemeRepository schemeRepo = mock(AmfiSchemeRepository.class);
        AmfiNavRepository navRepo = mock(AmfiNavRepository.class);
        var ingest = mock(com.sudheer.portfoliotracker.service.AmfiIngestService.class);

        AmfiScheme active = new AmfiScheme(); active.setSchemeCode("A"); active.setActive(Boolean.TRUE);
        AmfiScheme inactive = new AmfiScheme(); inactive.setSchemeCode("B"); inactive.setActive(Boolean.FALSE);

        when(schemeRepo.findByFundHouseContainingIgnoreCase("house"))
                .thenReturn(List.of(active, inactive));

        AmfiService svc = new AmfiService(schemeRepo, navRepo, ingest);

        List<AmfiScheme> all = svc.findByFundHouse("house", false);
        assertEquals(2, all.size());

        List<AmfiScheme> activeOnly = svc.findByFundHouse("house", true);
        assertEquals(1, activeOnly.size());
        assertEquals("A", activeOnly.get(0).getSchemeCode());
    }

    @Test
    void searchByName_returnsEmptyForNullOrBlankQuery_andReturnsRepositoryResultForValidQuery() {
        AmfiSchemeRepository schemeRepo = mock(AmfiSchemeRepository.class);
        AmfiNavRepository navRepo = mock(AmfiNavRepository.class);
        var ingest = mock(com.sudheer.portfoliotracker.service.AmfiIngestService.class);

        AmfiService svc = new AmfiService(schemeRepo, navRepo, ingest);

        assertTrue(svc.searchByName(null).isEmpty());
        assertTrue(svc.searchByName("   ").isEmpty());

        AmfiScheme s = new AmfiScheme(); s.setSchemeCode("X");
        when(schemeRepo.findBySchemeNameContainingIgnoreCase("query")).thenReturn(List.of(s));

        List<AmfiScheme> found = svc.searchByName("query");
        assertEquals(1, found.size());
        assertSame(s, found.get(0));
    }

    @Test
    void searchByCodePrefix_returnsEmptyForNullOrBlankQuery_andReturnsRepositoryResultForValidQuery() {
        AmfiSchemeRepository schemeRepo = mock(AmfiSchemeRepository.class);
        AmfiNavRepository navRepo = mock(AmfiNavRepository.class);
        var ingest = mock(com.sudheer.portfoliotracker.service.AmfiIngestService.class);

        AmfiService svc = new AmfiService(schemeRepo, navRepo, ingest);

        assertTrue(svc.searchByCodePrefix(null).isEmpty());
        assertTrue(svc.searchByCodePrefix("").isEmpty());

        AmfiScheme s = new AmfiScheme(); s.setSchemeCode("P123");
        when(schemeRepo.findBySchemeCodeStartingWith("P1")).thenReturn(List.of(s));

        List<AmfiScheme> found = svc.searchByCodePrefix("P1");
        assertEquals(1, found.size());
        assertSame(s, found.get(0));
    }

    @Test
    void getNav_delegatesToRepository_andReturnsOptional() {
        AmfiSchemeRepository schemeRepo = mock(AmfiSchemeRepository.class);
        AmfiNavRepository navRepo = mock(AmfiNavRepository.class);
        var ingest = mock(com.sudheer.portfoliotracker.service.AmfiIngestService.class);

        AmfiService svc = new AmfiService(schemeRepo, navRepo, ingest);

        LocalDate d = LocalDate.of(2021, 5, 20);
        AmfiNav nav = new AmfiNav(); nav.setSchemeCode("SC"); nav.setNavDate(d);
        when(navRepo.findBySchemeCodeAndNavDate("SC", d)).thenReturn(Optional.of(nav));

        Optional<AmfiNav> result = svc.getNav("SC", d);
        assertTrue(result.isPresent());
        assertSame(nav, result.get());

        when(navRepo.findBySchemeCodeAndNavDate("MISSING", d)).thenReturn(Optional.empty());
        Optional<AmfiNav> missing = svc.getNav("MISSING", d);
        assertTrue(missing.isEmpty());
    }

    @Test
    void triggerAdhocIngest_invokesIngestServiceFetchAndIngest() {
        AmfiSchemeRepository schemeRepo = mock(AmfiSchemeRepository.class);
        AmfiNavRepository navRepo = mock(AmfiNavRepository.class);
        var ingest = mock(com.sudheer.portfoliotracker.service.AmfiIngestService.class);

        AmfiService svc = new AmfiService(schemeRepo, navRepo, ingest);

        svc.triggerAdhocIngest();

        verify(ingest, times(1)).fetchAndIngest();
    }
}
