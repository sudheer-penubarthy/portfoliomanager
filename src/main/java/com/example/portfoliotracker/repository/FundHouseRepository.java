package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.FundHouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundHouseRepository extends JpaRepository<FundHouse, Long> {
    Optional<FundHouse> findByNameIgnoreCase(String name);
    Optional<FundHouse> findByName(String name);
    List<FundHouse> findByNameIn(List<String> names);
}
