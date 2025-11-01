package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    @Transactional
    public void fetchAndIngest() {
        String body = restTemplate.getForObject(amfiUrl, String.class);
        if (body == null || body.isEmpty()) return;

        List<String> lines = splitLines(body);
        if (lines.isEmpty()) return;

        // Detect and skip header if present
        int startIdx = 0;
        String firstLine = lines.get(0).toLowerCase();
        if (firstLine.contains("scheme code") || firstLine.contains("scheme_code")) {
            startIdx = 1;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

        for (int i = startIdx; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            // AMFI fields are semicolon delimited
            String[] cols = line.split(";", -1);
            // Typical columns:
            // 0: Scheme Code
            // 1: ISIN Div Payout/ISIN Growth
            // 2: ISIN Div Reinvestment (optional)
            // 3: Scheme Name
            // 4: Net Asset Value
            // 5: Date

            try {
                String schemeCode = cols.length > 0 ? cols[0].trim() : null;
                String isin1 = cols.length > 1 ? cols[1].trim() : null;
                String isin2 = cols.length > 2 ? cols[2].trim() : null;
                String schemeName = cols.length > 3 ? cols[3].trim() : null;
                String navStr = cols.length > 4 ? cols[4].trim() : null;
                String dateStr = cols.length > 5 ? cols[5].trim() : null;

                if (schemeCode == null || schemeCode.isEmpty()) continue;

                BigDecimal navValue = null;
                if (navStr != null && !navStr.isEmpty()) {
                    try {
                        navValue = new BigDecimal(navStr);
                    } catch (NumberFormatException e) {
                        // skip malformed nav but continue processing scheme
                        System.err.println("Malformed NAV value at line " + (i + 1) + ": " + navStr);
                        navValue = null;
                    }
                }

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

                // Upsert scheme (create if missing, update name if changed)
                Optional<AmfiScheme> existingSchemeOpt = schemeRepository.findBySchemeCode(schemeCode);
                AmfiScheme scheme;
                if (existingSchemeOpt.isPresent()) {
                    scheme = existingSchemeOpt.get();
                    if (schemeName != null && !schemeName.equals(scheme.getSchemeName())) {
                        scheme.setSchemeName(schemeName);
                        scheme.setUpdatedAt(java.time.LocalDateTime.now());
                        schemeRepository.save(scheme);
                    }
                } else {
                    scheme = new AmfiScheme();
                    scheme.setSchemeCode(schemeCode);
                    scheme.setIsinGrowth(isin2);
                    scheme.setIsinDividend(isin1);
                    scheme.setSchemeName(schemeName != null ? schemeName : "");
                    scheme.setFundHouse(null);
                    scheme.setInstrumentType(null);
                    scheme.setActive(true);
                    scheme.setMetadata(null);
                    scheme.setCreatedAt(java.time.LocalDateTime.now());
                    scheme.setUpdatedAt(java.time.LocalDateTime.now());
                    schemeRepository.save(scheme);
                }

                // Insert NAV if not exists (idempotent)
                if (navValue != null) {
                    boolean exists = navRepository.findBySchemeCodeAndNavDate(schemeCode, navDate).isPresent();
                    if (!exists) {
                        AmfiNav nav = new AmfiNav();
                        nav.setSchemeCode(schemeCode);
                        nav.setNavDate(navDate);
                        nav.setNavValue(navValue);
                        nav.setSource("AMFI");
                        nav.setCreatedAt(java.time.LocalDateTime.now());
                        navRepository.save(nav);
                    }
                }

            } catch (Exception e) {
                System.err.println("Error processing line " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    private List<String> splitLines(String body) {
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(body))) {
            String l;
            while ((l = br.readLine()) != null) {
                out.add(l);
            }
        } catch (Exception ex) {
            // ignore
        }
        return out;
    }
}
