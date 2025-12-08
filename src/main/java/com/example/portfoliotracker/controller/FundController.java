package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.dto.FundDto;
import com.example.portfoliotracker.service.FundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    @PostMapping
    public ResponseEntity<FundDto> create(@Valid @RequestBody FundDto dto) {
        FundDto created = fundService.create(dto);
        return ResponseEntity.created(URI.create("/api/funds/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FundDto> get(@PathVariable Long id) {
        return fundService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FundDto>> list() {
        return ResponseEntity.ok(fundService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FundDto> update(@PathVariable Long id, @Valid @RequestBody FundDto dto) {
        return ResponseEntity.ok(fundService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fundService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
