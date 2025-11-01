package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.service.AmfiIngestService;
import com.example.portfoliotracker.service.AmfiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/amfi")
public class AmfiController {
    private final AmfiService amfiService;
    private final AmfiIngestService ingestService;

    public AmfiController(AmfiIngestService ingestService, AmfiService amfiService) {
        this.ingestService = ingestService;
        this.amfiService = amfiService;
    }

    /**
     * GET /api/amfi/funds?fundHouse={fundHouse}&activeOnly=true
     */
    @GetMapping("/funds")
    public ResponseEntity<List<AmfiScheme>> getFundsByFundHouse(
            @RequestParam(required = false) String fundHouse,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly
    ) {
        List<AmfiScheme> result = amfiService.findByFundHouse(fundHouse, activeOnly);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/amfi/search?q={query}&by=schemeName|schemeCode
     */
    @GetMapping("/search")
    public ResponseEntity<List<AmfiScheme>> search(@RequestParam String q,
                                                   @RequestParam(defaultValue = "schemeName") String by) {
        List<AmfiScheme> result;
        if ("schemeCode".equalsIgnoreCase(by)) {
            result = amfiService.searchByCodePrefix(q);
        } else {
            result = amfiService.searchByName(q);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET NAV for a scheme on a date
     */
    @GetMapping("/nav/{schemeCode}")
    public ResponseEntity<AmfiNav> getNav(@PathVariable String schemeCode,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return amfiService.getNav(schemeCode, date)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    /**
     * POST /api/amfi/sync -> adhoc sync trigger (runs ingest immediately)
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync() {
        try {
            ingestService.fetchAndIngest();
            return ResponseEntity.ok("AMFI ingest triggered");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Failed to trigger ingest: " + ex.getMessage());
        }
    }

    @GetMapping("/ingest")
    public ResponseEntity<Void> ingest() {
        ingestService.fetchAndIngest();
        return ResponseEntity.noContent().build();
    }
}
