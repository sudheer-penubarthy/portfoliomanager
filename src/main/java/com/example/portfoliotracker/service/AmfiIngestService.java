package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiImport;
import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.entity.FundHouse;
import com.example.portfoliotracker.enums.Status;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.AmfiNavRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import com.example.portfoliotracker.repository.FundHouseRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
    // Tunables (can be externalized to application.yml)
    private static final int NAV_BATCH_SIZE = 1000;
    private static final int SCHEME_SAVE_CHUNK = 500;
    private static final int FUNDHOUSE_SAVE_CHUNK = 200;
    private static final int PROGRESS_STEP = 500;

    private final RestTemplate restTemplate;
    private final AmfiSchemeRepository schemeRepository;
    private final AmfiNavRepository navRepository;
    private final FundHouseRepository fundHouseRepository;
    private final AmfiPersistService persistService;
    private final String amfiUrl;
    private final AmfiImportRepository importRepository;

    public AmfiIngestService(RestTemplate restTemplate,
                             AmfiSchemeRepository schemeRepository,
                             AmfiNavRepository navRepository,
                             FundHouseRepository fundHouseRepository,
                             AmfiPersistService persistService,
                             @Value("${amfi.nav.url}") String amfiUrl, AmfiImportRepository importRepository) {
        this.restTemplate = restTemplate;
        this.schemeRepository = schemeRepository;
        this.navRepository = navRepository;
        this.fundHouseRepository = fundHouseRepository;
        this.persistService = persistService;
        this.amfiUrl = amfiUrl;
        this.importRepository = importRepository;
    }



    /**
     * Async trigger — non-blocking HTTP endpoint can call this
     */
    @Async
    public void fetchAndIngestAsync() {
        try {
            fetchAndIngest();
        } catch (Exception ex) {
            log.error("Async ingest failed", ex);
        }
    }

    /**
     * Main ingest orchestration (non-transactional). It will parse the file then
     * call persistService.persist*Chunk(...) to save data in small transactions.
     */
    public void fetchAndIngest() {
        log.info("Starting AMFI ingest orchestration");

        AmfiImport imp = AmfiImport.builder()
                .fileName("NAVAll.txt")                // or computed
                .sourceUrl(amfiUrl)
                .status(Status.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();
        imp = importRepository.save(imp);
        Long importId = imp.getId();

        String body = restTemplate.getForObject(amfiUrl, String.class);

        if (body == null || body.isEmpty()) {
            log.error("AMFI ingest failed: empty response from {}", amfiUrl);
            log.warn("AMFI source empty, aborting ingest");
            return;
        }

        List<String> lines = splitLines(body);
        if (lines.isEmpty()) {
            log.warn("AMFI source contains no lines");
            return;
        }
        // identify header row index if present
        int startIdx = 0;
        if (lines.get(0).toLowerCase().contains("scheme code")) {
            startIdx = 1;
        }

        String currentFundHouse = null;
        List<ParsedRow> parsedRows = new ArrayList<>();

        int processed = 0;
        int skipped = 0;

        for (int i = startIdx; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String[] cols = line.split(";", -1);
            // header handling (fund house vs category) — conservative heuristic
            if (cols.length < 2) {
                String header = line.trim();
                String lower = header.toLowerCase(Locale.ROOT);

                // detect obvious category lines
                boolean looksLikeCategory = lower.contains("open ended schemes") || lower.contains("close ended schemes") || lower.contains("schemes(") || lower.matches(".*schemes\\s*\\(.*\\).*");

                // detect likely fund-house names
                boolean looksLikeFundHouse = lower.contains("mutual fund") || lower.contains(" fund") // space before 'fund' reduces false positives
                        || lower.contains("asset management") || lower.contains("amc") || lower.contains("trust");

                if (looksLikeFundHouse && !looksLikeCategory) {
                    currentFundHouse = header;
                    log.debug("Switched to fund house: {}", currentFundHouse);
                } else {
                    // it's likely a category or other non-fundhouse header — skip it
                    log.debug("Skipping non-fundhouse header/category line: {}", header);
                }
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

                if (schemeCode.isEmpty()) {
                    skipped++;
                    continue;
                }

                LocalDate navDate = getLocalDate(dateStr);
                ParsedRow pr = ParsedRow.builder().
                        schemeCode(schemeCode)
                        .isin1(isin1)
                        .isin2(isin2)
                        .schemeName(schemeName)
                        .fundHouseName(currentFundHouse)
                        .navStr(navStr)
                        .navDate(navDate)
                        .build();
                parsedRows.add(pr);
            } catch (Exception e) {
                skipped++;
                log.error("Error processing line {}: {}", line, e.getMessage());
                //continue processing remaining lines
            }

        }
        log.info("Parsed {} rows (processed={}, skipped={})", parsedRows.size(), processed, skipped);
        if (parsedRows.isEmpty()) {
            log.info("No parsed rows to ingest");
            return;
        }

        //Collect distinct fund houses names and schema codes for bulk operations
        LinkedHashSet<String> fundHouseNames = parsedRows.stream()
                .map(ParsedRow::getFundHouseName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> schemaCodes = parsedRows.stream()
                .map(ParsedRow::getSchemeCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

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
                FundHouse fh = FundHouse.builder().name(fhName).build();
                newFundHouses.add(fh);
            }
        }

        // persist fund houses in chunks via persistService
        for (int i = 0; i < newFundHouses.size(); i += FUNDHOUSE_SAVE_CHUNK) {
            int end = Math.min(i + FUNDHOUSE_SAVE_CHUNK, newFundHouses.size());
            persistService.persistFundHousesChunk(newFundHouses.subList(i, end));
        }

        if (!newFundHouses.isEmpty()) {
            List<FundHouse> allFhs = fundHouseRepository.findByNameIn(new ArrayList<>(fundHouseNames));
            fhMap.clear();
            allFhs.forEach(fh -> fhMap.put(fh.getName(), fh));
        }

        // fetch existing schemes in bulk
        Map<String, AmfiScheme> schemeMap = new HashMap<>();
        if (!schemaCodes.isEmpty()) {
            log.debug("Fetching existing schemes for {} scheme codes", schemaCodes.size());
            List<AmfiScheme> existingSchemes = schemeRepository.findBySchemeCodeIn(new ArrayList<>(schemaCodes));
            schemeMap.putAll(existingSchemes.stream().collect(Collectors.toMap(AmfiScheme::getSchemeCode, s -> s)));
        }


        List<AmfiScheme> schemesToSave = new ArrayList<>();
        log.debug("Processing {} parsed rows for scheme/nav ingest", parsedRows.size());
        int newSchemes = 0;
        for (ParsedRow pr : parsedRows) {
            FundHouse fh = pr.getFundHouseName() != null ? fhMap.get(pr.getFundHouseName()) : null;
            AmfiScheme scheme = schemeMap.get(pr.getSchemeCode());
            if (scheme == null) {
                scheme = AmfiScheme.builder()
                        .schemeCode(pr.getSchemeCode())
                        .isinDividend(pr.getIsin1())
                        .isinGrowth(pr.getIsin2())
                        .schemeName(pr.getSchemeName() != null ? pr.getSchemeName() : "")
                        .fundHouse(pr.getFundHouseName())
                        .fundHouseEntity(fh)
                        .active(true)
                        .build();
                schemeMap.put(scheme.getSchemeCode(), scheme);
                schemesToSave.add(scheme);
                newSchemes++;
            } else {
                scheme.setSchemeName(pr.getSchemeName() != null ? pr.getSchemeName() : scheme.getSchemeName());
                scheme.setFundHouse(pr.getFundHouseName());
                scheme.setFundHouseEntity(fh);
                schemesToSave.add(scheme);
            }
        }

        // persist schemes in chunks via persistService
        for (int i = 0; i < schemesToSave.size(); i += SCHEME_SAVE_CHUNK) {
            int end = Math.min(i + SCHEME_SAVE_CHUNK, schemesToSave.size());
            persistService.persistSchemesChunk(schemesToSave.subList(i, end));
        }

        // prepare NAV entities
        List<AmfiNav> navsToSave = new ArrayList<>();
        for (ParsedRow pr : parsedRows) {
            if (pr.getNavStr() == null || pr.getNavStr().isEmpty()) continue;
            if (pr.getNavDate() == null) continue;
            try {
                // Skip if this nav already exists
                if (navRepository != null && navRepository.findBySchemeCodeAndNavDate(pr.getSchemeCode(), pr.getNavDate()).isPresent()) {
                    continue;
                }

                BigDecimal navValue = new BigDecimal(pr.getNavStr().replaceAll(",", ""));
                AmfiNav nav = AmfiNav.builder().schemeCode(pr.getSchemeCode()).navDate(pr.getNavDate()).navValue(navValue).source("AMFI").createdAt(LocalDateTime.now()).build();
                navsToSave.add(nav);

                // update fund house last date in-memory
                FundHouse fh = pr.getFundHouseName() != null ? fhMap.get(pr.getFundHouseName()) : null;
                if (fh != null && nav.getNavDate() != null) {
                    if (fh.getLastNavDate() == null || fh.getLastNavDate().isBefore(nav.getNavDate())) {
                        fh.setLastNavDate(nav.getNavDate());
                    }
                }
            } catch (NumberFormatException nfe) {
                log.warn("Skipping invalid NAV '{}' for scheme {}", pr.getNavStr(), pr.getSchemeCode());
            }
        }

        // persist navs in jdbc chunks via persistService
        for (int i = 0; i < navsToSave.size(); i += NAV_BATCH_SIZE) {
            int end = Math.min(i + NAV_BATCH_SIZE, navsToSave.size());
            persistService.persistNavsJdbcChunk(navsToSave.subList(i, end));
        }

        // persist fund house updates (last nav date) in chunks
        List<FundHouse> toUpdateFhs = fhMap.values().stream().filter(fh -> fh.getLastNavDate() != null).collect(Collectors.toList());
        for (int i = 0; i < toUpdateFhs.size(); i += FUNDHOUSE_SAVE_CHUNK) {
            int end = Math.min(i + FUNDHOUSE_SAVE_CHUNK, toUpdateFhs.size());
            persistService.persistFundHouseUpdatesChunk(toUpdateFhs.subList(i, end));
        }

        // summary logging
        log.info("AMFI ingest summary: processed={}, skipped={}, newSchemes={}, navRows={}", processed, skipped, newSchemes, navsToSave.size());
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