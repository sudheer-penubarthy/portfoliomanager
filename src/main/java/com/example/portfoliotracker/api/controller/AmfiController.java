package com.example.portfoliotracker.api.controller;

import com.example.portfoliotracker.infrastructure.persistence.entity.AmfiNav;
import com.example.portfoliotracker.infrastructure.persistence.entity.AmfiScheme;
import com.example.portfoliotracker.infrastructure.persistence.entity.FundHouse;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import com.example.portfoliotracker.repository.FundHouseRepository;
import com.example.portfoliotracker.service.AmfiIngestService;
import com.example.portfoliotracker.service.AmfiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/amfi")
public class AmfiController {
    private final AmfiService amfiService;
    private final AmfiIngestService ingestService;
    //private final FundHouseRepository fundHouseRepository;
    private final AmfiSchemeRepository schemeRepository;

    public AmfiController(AmfiService amfiService,
                          AmfiIngestService ingestService,
                          //FundHouseRepository fundHouseRepository,
                          AmfiSchemeRepository schemeRepository
                          ) {
        this.amfiService = amfiService;
        this.ingestService = ingestService;
        //this.fundHouseRepository = fundHouseRepository;
        this.schemeRepository = schemeRepository;
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
        ingestService.fetchAndIngestAsync();
        return ResponseEntity.ok("AMFI ingest triggered");
    }

    /**
     * List fund houses with scheme counts
     * Internally used by frontend; deprecated for external use
     * TODO: This will be replaced by SyncAmfiDataUseCase
     */
    @Deprecated
    @GetMapping("/fundhouses")
    public ResponseEntity<List<Map<String, ? extends Serializable>>> listFundHouses() {
        List<FundHouse> houses = amfiService.listFundHousesWithCounts();
        List<Map<String, ? extends Serializable>> payload = houses.stream().map(h -> Map.of(
                "id", h.getId(),
                "name", h.getName(),
                "schemeCount", schemeRepository.countByFundHouseEntity(h),
                "lastNavDate", h.getLastNavDate()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }
}
