package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.PortfolioUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PortfolioUserRepository extends JpaRepository<PortfolioUser, Long> {
    Optional<PortfolioUser> findByEmail(String email);
}
