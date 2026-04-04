package com.sudheer.portfoliotracker.service;

import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiScheme;
import com.sudheer.portfoliotracker.repository.AmfiSchemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Caches and pre-processes AMFI schemes for fast lookups.
 * Stores normalized names and tokenized versions to avoid repeated string operations.
 */
@Slf4j
@Service
public class SchemeRegistry {

    private final AmfiSchemeRepository schemeRepository;
    
    // Cached pre-processed scheme data
    private volatile SchemeCache schemeCache = null;

    public SchemeRegistry(AmfiSchemeRepository schemeRepository) {
        this.schemeRepository = schemeRepository;
        initializeCache();
    }

    /**
     * Initialize cache on startup
     */
    private void initializeCache() {
        log.debug("Initializing scheme cache...");
        refreshCache();
    }

    /**
     * Get cached all schemes as a map keyed by scheme code
     */
    public Map<String, AmfiScheme> getAllSchemesByCode() {
        ensureCacheIsValid();
        return schemeCache.schemesByCode();
    }

    /**
     * Get pre-tokenized lookup map for fuzzy matching
     */
    public Map<String, PreProcessedScheme> getPreProcessedSchemes() {
        ensureCacheIsValid();
        return schemeCache.preProcessedSchemes();
    }

    /**
     * Get all schemes as list
     */
    public List<AmfiScheme> getAllSchemes() {
        ensureCacheIsValid();
        return schemeCache.allSchemes();
    }

    /**
     * Force refresh cache (called when AMFI data is synced)
     */
    @CacheEvict(value = "amfiSchemes", allEntries = true)
    public void refreshCache() {
        log.info("Refreshing scheme cache from database...");
        
        List<AmfiScheme> allSchemes = schemeRepository.findAll();
        
        Map<String, AmfiScheme> byCode = allSchemes.stream()
                .collect(Collectors.toMap(AmfiScheme::getSchemeCode, scheme -> scheme, (left, right) -> left));
        
        Map<String, PreProcessedScheme> preProcessed = allSchemes.stream()
                .collect(Collectors.toMap(
                        AmfiScheme::getSchemeCode,
                        scheme -> new PreProcessedScheme(
                                scheme,
                                normalizeSchemeName(scheme.getSchemeName()),
                                tokenizeSchemeName(normalizeSchemeName(scheme.getSchemeName()))
                        ),
                        (left, right) -> left
                ));
        
        this.schemeCache = new SchemeCache(allSchemes, byCode, preProcessed);
        log.info("Scheme cache refreshed with {} schemes", allSchemes.size());
    }

    /**
     * Get scheme by code (from cache)
     */
    @Cacheable(value = "amfiSchemes", key = "'scheme:' + #code", unless = "#result == null")
    public AmfiScheme getSchemeByCode(String code) {
        ensureCacheIsValid();
        return schemeCache.schemesByCode().get(code);
    }

    /**
     * Check if cache is valid and refresh if needed
     */
    private void ensureCacheIsValid() {
        if (schemeCache == null) {
            refreshCache();
        }
    }

    /**
     * Normalize scheme name for comparison
     */
    private String normalizeSchemeName(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Tokenize normalized scheme name
     */
    private Set<String> tokenizeSchemeName(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> tokens = new HashSet<>();
        for (String token : normalizedName.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            if (token.length() <= 2) {
                continue;
            }
            if (token.equals("fund") || token.equals("plan") || token.equals("option")) {
                continue;
            }
            if (token.length() <= 5 && token.chars().allMatch(Character::isLetter)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    /**
     * Inner class to hold pre-processed scheme data
     */
    public record PreProcessedScheme(AmfiScheme scheme, String normalizedName, Set<String> tokens) {
    }

    /**
     * Inner class to hold cached scheme data
     */
    private record SchemeCache(
            List<AmfiScheme> allSchemes,
            Map<String, AmfiScheme> schemesByCode,
            Map<String, PreProcessedScheme> preProcessedSchemes
    ) {
        private SchemeCache {
            allSchemes = List.copyOf(allSchemes);
            schemesByCode = Map.copyOf(schemesByCode);
            preProcessedSchemes = Map.copyOf(preProcessedSchemes);
        }
    }
}

