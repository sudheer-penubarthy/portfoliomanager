package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.FundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRepository extends JpaRepository<FundEntity, Long> { }
