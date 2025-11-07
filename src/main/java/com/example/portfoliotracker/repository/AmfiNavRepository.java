package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.AmfiNav;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AmfiNavRepository extends JpaRepository<AmfiNav, Long> {
    Optional<AmfiNav> findBySchemeCodeAndNavDate(String schemeCode, LocalDate navDate);

}
