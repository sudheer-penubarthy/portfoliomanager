package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.entity.FundHouse;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import com.example.portfoliotracker.repository.FundHouseRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AmfiIngestService {
    private final RestTemplate restTemplate;
    private final AmfiSchemeRepository schemeRepository;
    private final AmfiNavRepository navRepository;
    private final String amfiUrl;
    private final FundHouseRepository fundHouseRepository;

    public AmfiIngestService(RestTemplate restTemplate, AmfiSchemeRepository schemeRepository, AmfiNavRepository navRepository, @Value("${amfi.nav.url}") String amfiUrl, FundHouseRepository fundHouseRepository) {
        this.restTemplate = restTemplate;
        this.schemeRepository = schemeRepository;
        this.navRepository = navRepository;
        this.amfiUrl = amfiUrl;
        this.fundHouseRepository = fundHouseRepository;
    }

    /**
     * Download the NAVAll.txt from AMFI and persist schemes and navs.
     * This method groups saves per fund house and uses saveAll for faster inserts.
     * This method is idempotent: duplicate (scheme_code, nav_date) rows are skipped.
     * Two-pass ingest for better performance:
     * * 1) parse file into ParsedRow list
     * * 2) bulk insert missing FundHouse rows (saveAll),
     * * 3) bulk upsert AmfiScheme rows (saveAll),
     * * 4) insert NAV rows in batch (saveAll)
     */
    @Transactional
    public void fetchAndIngest() {
        String body = restTemplate.getForObject(amfiUrl, String.class);
        if (body == null || body.isEmpty()) {
            log.error("AMFI ingest failed: empty response from {}", amfiUrl);
            return;
        }
        List<String> lines = splitLines(body);
        if (lines.isEmpty()) {
            log.error("AMFI ingest failed: no data lines from {}", amfiUrl);
            return;
        }
        int startIndex = 0;
        // Skip header lines
        if (lines.get(0).startsWith("Scheme Code") || lines.get(0).toLowerCase().contains("scheme_code")) {
            startIndex = 1;
        }

        String currentFundhouse = null;
        List<ParsedRow> parsedRows = new ArrayList<>();

        int processed = 0;
        int skipped = 0;
        int newSchemes = 0;

        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || line.isEmpty()) {
                continue;
            }

            String[] cols = line.split(";", -1);
            // If it's a fund house header
            if (cols.length < 2) {
                currentFundhouse = line.trim();
                log.debug("Switched to fund house: {}", currentFundhouse);
                continue;
            }
            processed++;
            try {
                //  Now it's a valid scheme entry
                String schemeCode = cols[0].trim();
                String isin1 = cols[1].trim();
                String isin2 = cols.length > 2 ? cols[2].trim() : null;
                String schemeName = cols.length > 3 ? cols[3].trim() : null;
                String navStr = cols.length > 4 ? cols[4].trim() : null;
                String dateStr = cols.length > 5 ? cols[5].trim() : null;

                if (schemeCode == null || schemeCode.isEmpty()) {
                    skipped++;
                    continue;
                }

                LocalDate navDate = getLocalDate(dateStr);
                ParsedRow pr = ParsedRow.builder().schemeCode(schemeCode).isin1(isin1).isin2(isin2).schemeName(schemeName).fundHouseName(currentFundhouse).navStr(navStr).navDate(navDate).build();

                parsedRows.add(pr);


            } catch (Exception e) {
                skipped++;
                log.error("Error processing line {}: {}", line, e.getMessage());
                //continue processing remaining lines
            }
        }

        //Collect distinct fund houses names and schema codes for bulk operations
        LinkedHashSet<String> fundHouseNames = parsedRows.stream().map(ParsedRow::getFundHouseName).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedHashSet<String> schemaCodes = parsedRows.stream().map(ParsedRow::getSchemeCode).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

        //fetch existing fund houses in bulk
        Map<String, FundHouse> fhMap = new HashMap<>();
        if (!fundHouseNames.isEmpty()) {
            List<FundHouse> existingHouses = fundHouseRepository.findByNameIn(new ArrayList<>(fundHouseNames));
            fhMap.putAll(existingHouses.stream().collect(Collectors.toMap(FundHouse::getName, fh -> fh)));
        }

        List<FundHouse> newFundHouses = new ArrayList<>();
        for (String fhName : fundHouseNames) {
            if (fhName == null) continue;
            if (!fhMap.containsKey(fhName)) {
                FundHouse fh = FundHouse.builder().name(fhName).createdAt(LocalDateTime.now()).build();
                newFundHouses.add(fh);
            }
        }

        if (!newFundHouses.isEmpty()) {
            List<FundHouse> savedHouses = fundHouseRepository.saveAll(newFundHouses);
            savedHouses.forEach(fh -> fhMap.put(fh.getName(), fh));
            log.debug("Saved {} new fund houses", savedHouses.size());
        }

        // fetch existing schemes in bulk
        Map<String, AmfiScheme> schemeMap = new HashMap<>();
        if (!schemaCodes.isEmpty()) {
            List<AmfiScheme> existingSchemes = schemeRepository.findBySchemeCodeIn(new ArrayList<>(schemaCodes));
            schemeMap.putAll(existingSchemes.stream().collect(Collectors.toMap(AmfiScheme::getSchemeCode, s -> s)));
        }

        List<AmfiScheme> schemesToSave = new ArrayList<>();
        List<AmfiNav> navsToSave = new ArrayList<>();

        for (ParsedRow pr : parsedRows) {
            FundHouse fh = pr.getFundHouseName() != null ? fhMap.get(pr.getFundHouseName()) : null;
            AmfiScheme scheme = schemeMap.get(pr.getSchemeCode());
            if (scheme == null) {
                // new scheme
                scheme = AmfiScheme.builder().schemeCode(pr.getSchemeCode()).isinDividend(pr.getIsin1()).isinGrowth(pr.getIsin2()).schemeName(pr.getSchemeName()).fundHouseEntity(fh).fundHouse(pr.getFundHouseName()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).active(true).build();
                schemeMap.put(scheme.getSchemeCode(), scheme);
                schemesToSave.add(scheme);
                newSchemes++;
            } else {
                // existing scheme - update details if needed
                scheme.setSchemeName(pr.getSchemeName() != null ? pr.getSchemeName() : scheme.getSchemeName());
                scheme.setFundHouse(pr.getFundHouseName());
                scheme.setFundHouseEntity(fh);
                scheme.setUpdatedAt(LocalDateTime.now());
                schemesToSave.add(scheme);
            }

            // prepare NAV entry
            if (pr.getNavStr() != null && !pr.getNavStr().isEmpty()) {
                try {
                    BigDecimal navValue = new BigDecimal(pr.getNavStr().replaceAll(",", ""));
                    // check if NAV for this scheme and date already exists
                    AmfiNav nav = AmfiNav.builder().schemeCode(pr.getSchemeCode()).navDate(pr.getNavDate() != null ? pr.getNavDate() : LocalDate.now()).navValue(navValue).createdAt(LocalDateTime.now()).build();
                    boolean exists = navRepository.findBySchemeCodeAndNavDate(nav.getSchemeCode(), nav.getNavDate()).isPresent();
                    if (!exists) {
                        navsToSave.add(nav);
                    }
                    if (fh != null && nav.getNavDate() != null) {
                        //update last nav date for fund house
                        if (fh.getLastNavDate() == null || nav.getNavDate().isAfter(fh.getLastNavDate())) {
                            fh.setLastNavDate(nav.getNavDate());
                        }
                    }
                } catch (NumberFormatException nfe) {
                    log.warn("Skipping invalid NAV value '{}' for scheme code {} on date {}", pr.getNavStr(), pr.getSchemeCode(), pr.getNavDate());
                }
            }

            //batch save schemes
            if (!schemesToSave.isEmpty()) {
                schemeRepository.saveAll(schemesToSave);
                log.debug("Saved/updated {} schemes", schemesToSave.size());
            }
            //batch save navs
            if (!navsToSave.isEmpty()) {
                navRepository.saveAll(navsToSave);
                log.debug("Saved {} NAV rows", navsToSave.size());
            }
            // batch update fund houses last nav date
            List<FundHouse> toUpdateFhs = fhMap.values().stream().filter(f -> f.getLastNavDate() != null).collect(Collectors.toList());

            if (!toUpdateFhs.isEmpty()) {
                fundHouseRepository.saveAll(toUpdateFhs);
                log.debug("Updated {} fund houses with last NAV date", toUpdateFhs.size());
            }

            // summary logging
            log.info("AMFI ingest summary: processed={}, skipped={}, newSchemes={}, navRows={}", processed, skipped, newSchemes, navsToSave.size());

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

    private LocalDate getLocalDate(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
        LocalDate navDate;
        if ((dateStr != null) && !dateStr.isEmpty()) {
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

    @Data
    @Builder
    static class ParsedRow {
        String schemeCode;
        String isin1;
        String isin2;
        String schemeName;
        String fundHouseName;
        String navStr;
        LocalDate navDate;
    }


}
