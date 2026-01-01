package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.dto.FundDto;
import com.example.portfoliotracker.service.FundService;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/funds")
@Slf4j
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    /**
     * Create a new fund
     * @param dto the fund data
     * @return 201 Created with location header and fund data
     */
    @PostMapping
    public ResponseEntity<FundDto> create(@Valid @RequestBody FundDto dto) {
        log.info("Creating fund: {}", dto);
        FundDto created = fundService.create(dto);
        log.debug("Fund created with ID: {}", created.getId());
        return ResponseEntity.created(URI.create("/api/funds/" + created.getId())).body(created);
    }

    /**
     * Get a fund by ID
     * @param id the fund ID
     * @return 200 OK with fund data, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<FundDto> get(@PathVariable Long id) {
        log.debug("Fetching fund with ID: {}", id);
        return fundService.findById(id)
                .map(fund -> {
                    log.debug("Fund found with ID: {}", id);
                    return ResponseEntity.ok(fund);
                })
                .orElseGet(() -> {
                    log.warn("Fund not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get all funds
     * @return 200 OK with list of funds
     */
    @GetMapping
    public ResponseEntity<List<FundDto>> list() {
        log.debug("Fetching all funds");
        List<FundDto> funds = fundService.findAll();
        log.debug("Retrieved {} funds", funds.size());
        return ResponseEntity.ok(funds);
    }

    /**
     * Update a fund
     * @param id the fund ID
     * @param dto the updated fund data
     * @return 200 OK with updated fund data, or 404 Not Found
     */
    @PutMapping("/{id}")
    public ResponseEntity<FundDto> update(@PathVariable Long id, @Valid @RequestBody FundDto dto) {
        log.info("Updating fund with ID: {}", id);
        return fundService.findById(id)
                .map(existingFund -> {
                    FundDto updated = fundService.update(id, dto);
                    log.debug("Fund updated successfully with ID: {}", id);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    log.warn("Cannot update - Fund not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Delete a fund
     * @param id the fund ID
     * @return 204 No Content, or 404 Not Found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting fund with ID: {}", id);
        if (fundService.findById(id).isPresent()) {
            fundService.delete(id);
            log.debug("Fund deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Cannot delete - Fund not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
