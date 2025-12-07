package com.example.portfoliotracker.service.impl;

import com.example.portfoliotracker.dto.FundDto;
import com.example.portfoliotracker.entity.FundEntity;
import com.example.portfoliotracker.exception.ResourceNotFoundException;
import com.example.portfoliotracker.mapper.PortfolioMapper;
import com.example.portfoliotracker.repository.FundRepository;
import com.example.portfoliotracker.service.FundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FundServiceImpl implements FundService {

    private final FundRepository fundRepository;
    private final PortfolioMapper mapper;

    public FundServiceImpl(FundRepository fundRepository, PortfolioMapper mapper) {
        this.fundRepository = fundRepository;
        this.mapper = mapper;
    }

    @Override
    public FundDto create(FundDto dto) {
        FundEntity entity = mapper.dtoToEntity(dto);
        FundEntity saved = fundRepository.save(entity);
        return mapper.entityToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FundDto> findById(Long id) {
        return fundRepository.findById(id).map(mapper::entityToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundDto> findAll() {
        return fundRepository.findAll().stream().map(mapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public FundDto update(Long id, FundDto dto) {
        FundEntity existing = fundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund", "id", id));
        // map fields from dto -> existing (simple approach: create mapped entity and set id)
        FundEntity updated = mapper.dtoToEntity(dto);
        updated.setId(existing.getId());
        return mapper.entityToDto(fundRepository.save(updated));
    }

    @Override
    public void delete(Long id) {
        fundRepository.deleteById(id);
    }
}
