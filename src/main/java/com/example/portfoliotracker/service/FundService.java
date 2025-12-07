package com.example.portfoliotracker.service;

import com.example.portfoliotracker.dto.FundDto;
import java.util.List;
import java.util.Optional;

public interface FundService {
    FundDto create(FundDto dto);
    Optional<FundDto> findById(Long id);
    List<FundDto> findAll();
    FundDto update(Long id, FundDto dto);
    void delete(Long id);
}
