package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AmfiService {
    private final AmfiSchemeRepository schemeRepository;
    private final AmfiNavRepository navRepository;
    private final AmfiIngestService ingestService;


    public AmfiService(AmfiSchemeRepository schemeRepository, AmfiNavRepository navRepository, AmfiIngestService ingestService) {
        this.schemeRepository = schemeRepository;
        this.navRepository = navRepository;
        this.ingestService = ingestService;
    }

    public List<AmfiScheme> findByFundHouse(String fundHouse, boolean activeOnly) {
        if (fundHouse == null || fundHouse.isBlank()) {
            return schemeRepository.findAll();
        }
        List<AmfiScheme> list = schemeRepository.findByFundHouseContainingIgnoreCase(fundHouse);
        if (!activeOnly) return list;
        return list.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).toList();
    }

    public List<AmfiScheme> searchByName(String q) {
        if (q == null || q.isBlank()) return List.of();
        return schemeRepository.findBySchemeNameContainingIgnoreCase(q);
    }

    public List<AmfiScheme> searchByCodePrefix(String q) {
        if (q == null || q.isBlank()) return List.of();
        return schemeRepository.findBySchemeCodeStartingWith(q);
    }

    public Optional<AmfiNav> getNav(String schemeCode, LocalDate date) {
        return navRepository.findBySchemeCodeAndNavDate(schemeCode, date);
    }

    public void triggerAdhocIngest() {
        ingestService.fetchAndIngest();
    }
}
