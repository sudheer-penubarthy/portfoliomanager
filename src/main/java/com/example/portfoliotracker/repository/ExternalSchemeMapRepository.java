package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.ExternalSchemeMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalSchemeMapRepository extends JpaRepository<ExternalSchemeMap, Long> {
    Optional<ExternalSchemeMap> findByRtaNameAndExternalCode(String rtaName, String externalCode);
}
