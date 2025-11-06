package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class AmfiIngestService {
    private final RestTemplate restTemplate;
    private final AmfiSchemeRepository schemeRepository;
    private final AmfiNavRepository navRepository;
    private final String amfiUrl;

    public AmfiIngestService(RestTemplate restTemplate, AmfiSchemeRepository schemeRepository, AmfiNavRepository navRepository, @Value("${amfi.nav.url}") String amfiUrl) {
        this.restTemplate = restTemplate;
        this.schemeRepository = schemeRepository;
        this.navRepository = navRepository;
        this.amfiUrl = amfiUrl;
    }

    /**
     * Download the NAVAll.txt from AMFI and persist schemes and navs.
     * This method is idempotent: duplicate (scheme_code, nav_date) rows are skipped.
     */

    public void fetchAndIngest() {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new URL("https://portal.amfiindia.com/spages/NAVAll.txt").openStream()))) {

            String line;
            String currentFundhouse = null;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("Scheme Code")) {
                    continue; // skip headers and empty lines
                }
                String[] cols = line.split(";", -1);

                // If it's a fund house header
                if (cols.length < 2) {
                    currentFundhouse = line.trim();
                    log.debug("Switched to fund house: {}", currentFundhouse);
                    continue;
                }

                //  Now it's a valid scheme entry
                String schemeCode = cols[0].trim();
                String isin1 = cols[1].trim();
                String isin2 = cols.length > 2 ? cols[2].trim() : null;
                String schemeName = cols.length > 3 ? cols[3].trim() : null;
                String navStr = cols.length > 4 ? cols[4].trim() : null;
                String dateStr = cols.length > 5 ? cols[5].trim() : null;

                if (schemeCode.isEmpty()) continue;

                // Upsert scheme
                String fundHouse = currentFundhouse;
                LocalDate navDate = getLocalDate(dateStr);
                AmfiScheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                        .orElseGet(() -> {
                            log.debug("New scheme: {} ({})", schemeName, schemeCode);
                            AmfiScheme s = new AmfiScheme();
                            s.setSchemeCode(schemeCode);
                            s.setSchemeName(schemeName);
                            s.setFundHouse(fundHouse); // attach current fund house
                            s.setActive(true);
                            s.setCreatedAt(LocalDateTime.now());
                            return s;
                        });

                // update only if new name or inactive
                scheme.setSchemeName(schemeName);
                scheme.setFundHouse(currentFundhouse);
                scheme.setUpdatedAt(LocalDateTime.now());
                schemeRepository.save(scheme);

                // Parse NAV
                try {
                    if (navStr != null && dateStr != null) {
                        BigDecimal nav = new BigDecimal(navStr);
                        AmfiNav amfiNav = new AmfiNav();
                        amfiNav.setSchemeCode(schemeCode);
                        amfiNav.setNavValue(nav);
                        amfiNav.setNavDate(navDate);
                        amfiNav.setCreatedAt(LocalDateTime.now());
                        navRepository.save(amfiNav);
                    }
                } catch (Exception ex) {
                    log.warn("Skipping NAV parse for {} ({})", schemeCode, schemeName, ex);
                }
                log.info("AMFI ingest completed. Total Schemes: {}, Fund Houses: {}",
                        schemeRepository.count(),
                        schemeRepository.findDistinctFundHouses().size());

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception ex) {
            log.error("AMFI ingest failed", ex);
        }

    }

    private static LocalDate getLocalDate(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
        LocalDate navDate;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                navDate = LocalDate.parse(dateStr, formatter);
            } catch (Exception ex) {
                try {
                    navDate = LocalDate.parse(dateStr);
                } catch (Exception ex2) {
                    navDate = LocalDate.now();
                }
            }
        } else {
            navDate = LocalDate.now();
        }
        return navDate;
    }

}
