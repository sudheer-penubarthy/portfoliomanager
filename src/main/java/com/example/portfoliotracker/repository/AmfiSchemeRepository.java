package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.entity.FundHouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmfiSchemeRepository extends JpaRepository<AmfiScheme, Long> {
    List<AmfiScheme> findByFundHouseContainingIgnoreCase(String fundHouse);
    List<AmfiScheme> findBySchemeCodeStartingWith(String codePrefix);
    List<AmfiScheme> findBySchemeNameContainingIgnoreCase(String name);
    Optional<AmfiScheme> findBySchemeCode(String schemeCode);
    @Query("SELECT COUNT(s) FROM AmfiScheme s WHERE s.fundHouseEntity = :fundHouse")
    long countByFundHouseEntity(FundHouse fundHouse);
    // bulk fetch
    List<AmfiScheme> findBySchemeCodeIn(List<String> schemeCodes);
}